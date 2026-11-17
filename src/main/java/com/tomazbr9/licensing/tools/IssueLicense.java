package com.tomazbr9.licensing.tools;

import com.tomazbr9.licensing.License;
import com.tomazbr9.licensing.LicenseFileStore;
import com.tomazbr9.licensing.LicensePayloadCodec;
import com.tomazbr9.licensing.LicensePublicKeyLoader;
import com.tomazbr9.licensing.LicenseType;
import com.tomazbr9.licensing.SignedLicense;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import java.security.spec.PKCS8EncodedKeySpec;

import java.time.LocalDate;

import java.util.Arrays;
import java.util.Base64;

public final class IssueLicense {

    private static final String KEY_ID =
            "AUTO_NOTA_KEY_01";

    private IssueLicense() {
    }

    public static void main(String[] args)
            throws Exception {

        if (args.length != 5) {

            System.err.println(
                    "Uso: IssueLicense "
                            + "<chave-privada.pk8> "
                            + "<licenseId> "
                            + "<cliente> "
                            + "<installationId> "
                            + "<arquivo.lic>"
            );

            System.exit(1);
        }

        Path arquivoPrivado = Path.of(args[0])
                .toAbsolutePath()
                .normalize();

        String licenseId = args[1];
        String cliente = args[2];
        String installationId = args[3];

        Path destino = Path.of(args[4])
                .toAbsolutePath()
                .normalize();

        // Nao permitir arquivos de licenca
        // dentro do repositorio.

        Path projeto = Path.of("")
                .toAbsolutePath()
                .normalize();

        if (destino.startsWith(projeto)) {
            throw new IllegalArgumentException(
                    "Salve as licencas fora "
                            + "da pasta do projeto."
            );
        }

        if (!destino.getFileName()
                .toString()
                .endsWith(".lic")) {

            throw new IllegalArgumentException(
                    "O arquivo deve ter extensao .lic."
            );
        }

        if (!Files.isRegularFile(arquivoPrivado)) {
            throw new IllegalArgumentException(
                    "Chave privada nao encontrada."
            );
        }

        // 1. Cria a licenca vitalicia

        License license = new License(
                License.CURRENT_SCHEMA_VERSION,
                licenseId,
                License.PRODUCT_ID,
                cliente,
                installationId,
                LocalDate.now(),
                LicenseType.PERPETUAL
        );

        // 2. Carrega a chave privada PKCS#8

        byte[] privadaPkcs8 =
                Files.readAllBytes(arquivoPrivado);

        PrivateKey chavePrivada;

        try {
            chavePrivada = KeyFactory
                    .getInstance("Ed25519")
                    .generatePrivate(
                            new PKCS8EncodedKeySpec(
                                    privadaPkcs8
                            )
                    );
        } finally {
            Arrays.fill(
                    privadaPkcs8,
                    (byte) 0
            );
        }

        // 3. Carrega a chave publica do Auto-Nota

        PublicKey chavePublica =
                LicensePublicKeyLoader.carregar();

        // 4. Serializa o conteudo da licenca

        byte[] payload =
                LicensePayloadCodec.codificar(license);

        // 5. Assina com Ed25519

        Signature assinador =
                Signature.getInstance("Ed25519");

        assinador.initSign(chavePrivada);
        assinador.update(payload);

        byte[] assinatura = assinador.sign();

        // 6. Confirma que a chave privada
        // corresponde a publica do aplicativo.

        if (!verificar(
                chavePublica,
                payload,
                assinatura
        )) {

            throw new IllegalStateException(
                    "A chave privada nao corresponde "
                            + "a chave publica do Auto-Nota."
            );
        }

        // 7. Monta o arquivo assinado

        SignedLicense assinada = new SignedLicense(
                Base64.getEncoder()
                        .encodeToString(payload),

                Base64.getEncoder()
                        .encodeToString(assinatura),

                KEY_ID
        );

        // 8. Salva o arquivo .lic

        LicenseFileStore.salvar(
                destino,
                assinada
        );

        // 9. Le e confere o arquivo gravado

        SignedLicense recuperada =
                LicenseFileStore.ler(destino);

        byte[] conteudoLido =
                Base64.getDecoder().decode(
                        recuperada.payloadBase64()
                );

        byte[] assinaturaLida =
                Base64.getDecoder().decode(
                        recuperada.signatureBase64()
                );

        boolean chaveCorreta =
                KEY_ID.equals(recuperada.keyId());

        boolean assinaturaValida = verificar(
                chavePublica,
                conteudoLido,
                assinaturaLida
        );

        boolean dadosCorretos = license.equals(
                LicensePayloadCodec.decodificar(
                        conteudoLido
                )
        );

        if (!chaveCorreta
                || !assinaturaValida
                || !dadosCorretos) {

            throw new IllegalStateException(
                    "O arquivo gerado nao passou "
                            + "na verificacao."
            );
        }

        // 10. Resultado

        System.out.println(
                "Licenca emitida e assinatura validada."
        );

        System.out.println(
                "ID: " + license.licenseId()
        );

        System.out.println(
                "Cliente: " + license.customerName()
        );

        System.out.println(
                "Instalacao: " + license.installationId()
        );

        System.out.println(
                "Tipo: " + license.type()
        );

        System.out.println(
                "Arquivo: " + destino
        );
    }

    private static boolean verificar(
            PublicKey publica,
            byte[] payload,
            byte[] assinatura
    ) throws Exception {

        Signature verificador =
                Signature.getInstance("Ed25519");

        verificador.initVerify(publica);
        verificador.update(payload);

        return verificador.verify(assinatura);
    }
}
