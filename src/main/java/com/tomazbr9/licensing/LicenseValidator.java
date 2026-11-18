package com.tomazbr9.licensing;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.DateTimeException;
import java.util.Base64;
import java.util.Objects;

public final class LicenseValidator {

    private static final String KEY_ID =
            "AUTO_NOTA_KEY_01";

    private static final int MAX_PAYLOAD_BYTES =
            16 * 1024;

    private static final int ED25519_SIGNATURE_BYTES =
            64;

    private final PublicKey chavePublica;

    public LicenseValidator(PublicKey chavePublica) {
        this.chavePublica = Objects.requireNonNull(
                chavePublica,
                "Chave publica obrigatoria."
        );
    }

    public static LicenseValidator criarPadrao()
            throws IOException, GeneralSecurityException {

        return new LicenseValidator(
                LicensePublicKeyLoader.carregar()
        );
    }

    public LicenseValidationResult validar(Path arquivo) {

        if (arquivo == null) {
            return LicenseValidationResult.rejeitada(
                    "Arquivo de licenca nao informado."
            );
        }

        try {
            // 1. Ler o arquivo .lic
            SignedLicense assinada =
                    LicenseFileStore.ler(arquivo);

            // 2. Conferir a identificacao da chave
            if (!KEY_ID.equals(assinada.keyId())) {
                return LicenseValidationResult.rejeitada(
                        "Identificador de chave nao reconhecido."
                );
            }

            // 3. Decodificar o conteudo e a assinatura
            byte[] payload =
                    Base64.getDecoder().decode(
                            assinada.payloadBase64()
                    );

            byte[] assinatura =
                    Base64.getDecoder().decode(
                            assinada.signatureBase64()
                    );

            // 4. Rejeitar tamanhos invalidos
            if (payload.length == 0
                    || payload.length > MAX_PAYLOAD_BYTES
                    || assinatura.length
                    != ED25519_SIGNATURE_BYTES) {

                return LicenseValidationResult.rejeitada(
                        "Tamanho de conteudo ou assinatura invalido."
                );
            }

            // 5. Verificar a assinatura Ed25519
            Signature verificador =
                    Signature.getInstance("Ed25519");

            verificador.initVerify(chavePublica);
            verificador.update(payload);

            if (!verificador.verify(assinatura)) {
                return LicenseValidationResult.rejeitada(
                        "Assinatura digital invalida."
                );
            }

            // 6. Somente interpretar os dados
            // depois de validar a assinatura
            License licenca =
                    LicensePayloadCodec.decodificar(payload);

            // As regras de produto, versao e tipo
            // sao verificadas pelo construtor
            // do record License.

            return LicenseValidationResult.aprovada(
                    licenca
            );

        } catch (IOException e) {

            return LicenseValidationResult.rejeitada(
                    "Nao foi possivel ler ou interpretar "
                            + "a licenca: "
                            + e.getMessage()
            );

        } catch (IllegalArgumentException
                 | DateTimeException e) {

            return LicenseValidationResult.rejeitada(
                    "Arquivo de licenca com codificacao "
                            + "ou campos invalidos."
            );

        } catch (GeneralSecurityException e) {

            return LicenseValidationResult.rejeitada(
                    "Falha na verificacao criptografica: "
                            + e.getMessage()
            );
        }
    }
}
