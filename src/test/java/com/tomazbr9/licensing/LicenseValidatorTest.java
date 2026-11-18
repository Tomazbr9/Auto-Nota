package com.tomazbr9.licensing;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;

import java.time.LocalDate;
import java.util.Base64;

public class LicenseValidatorTest {

    public static void main(String[] args)
            throws Exception {

        // 1. Criar chaves temporarias
        KeyPair par =
                KeyPairGenerator.getInstance("Ed25519")
                        .generateKeyPair();

        LicenseValidator validator =
                new LicenseValidator(par.getPublic());

        // 2. Criar licenca de teste
        License license = new License(
                License.CURRENT_SCHEMA_VERSION,
                "AN-TEST-0001",
                License.PRODUCT_ID,
                "Posto Exemplo",
                "TEST-INSTALLATION-001",
                LocalDate.of(2026, 11, 18),
                LicenseType.PERPETUAL
        );

        byte[] payload =
                LicensePayloadCodec.codificar(license);

        byte[] assinatura =
                assinar(par.getPrivate(), payload);

        Path pasta = Files.createTempDirectory(
                "auto-nota-validator-"
        );

        try {
            // TESTE 1 - Licenca valida
            Path valido = pasta.resolve("valido.lic");

            LicenseFileStore.salvar(
                    valido,
                    montar(
                            payload,
                            assinatura,
                            "AUTO_NOTA_KEY_01"
                    )
            );

            LicenseValidationResult resultado =
                    validator.validar(valido);

            conferir(
                    resultado.valid(),
                    "Licenca autentica deveria ser aceita."
            );

            conferir(
                    license.equals(resultado.license()),
                    "Dados recuperados diferentes."
            );

            // TESTE 2 - Conteudo alterado
            byte[] alterado = payload.clone();
            alterado[10] ^= 1;

            Path fraude = pasta.resolve("alterado.lic");

            LicenseFileStore.salvar(
                    fraude,
                    montar(
                            alterado,
                            assinatura,
                            "AUTO_NOTA_KEY_01"
                    )
            );

            conferir(
                    !validator.validar(fraude).valid(),
                    "Alteracao do payload foi aceita."
            );

            // TESTE 3 - Assinatura alterada
            byte[] assinaturaAlterada = assinatura.clone();
            assinaturaAlterada[0] ^= 1;

            Path assinaturaInvalida = pasta.resolve(
                    "assinatura-invalida.lic"
            );

            LicenseFileStore.salvar(
                    assinaturaInvalida,
                    montar(
                            payload,
                            assinaturaAlterada,
                            "AUTO_NOTA_KEY_01"
                    )
            );

            conferir(
                    !validator.validar(
                            assinaturaInvalida
                    ).valid(),
                    "Assinatura alterada foi aceita."
            );

            // TESTE 4 - Identificador de chave errado
            Path chaveErrada = pasta.resolve(
                    "chave-errada.lic"
            );

            LicenseFileStore.salvar(
                    chaveErrada,
                    montar(
                            payload,
                            assinatura,
                            "OUTRA_CHAVE"
                    )
            );

            conferir(
                    !validator.validar(
                            chaveErrada
                    ).valid(),
                    "Identificador desconhecido foi aceito."
            );

            // TESTE 5 - Base64 invalido
            Path base64Invalido = pasta.resolve(
                    "base64-invalido.lic"
            );

            LicenseFileStore.salvar(
                    base64Invalido,
                    new SignedLicense(
                            "!!!",
                            "!!!",
                            "AUTO_NOTA_KEY_01"
                    )
            );

            conferir(
                    !validator.validar(
                            base64Invalido
                    ).valid(),
                    "Base64 invalido foi aceito."
            );

            // TESTE 6 - Outra chave publica
            KeyPair outroPar =
                    KeyPairGenerator.getInstance(
                            "Ed25519"
                    ).generateKeyPair();

            LicenseValidator outroValidator =
                    new LicenseValidator(
                            outroPar.getPublic()
                    );

            conferir(
                    !outroValidator.validar(valido).valid(),
                    "Chave publica errada aceitou assinatura."
            );

            // TESTE 7 - Arquivo inexistente
            conferir(
                    !validator.validar(
                            pasta.resolve("nao-existe.lic")
                    ).valid(),
                    "Arquivo ausente foi aceito."
            );

            System.out.println(
                    "OK: licenca autentica aceita; "
                            + "arquivos alterados, chave "
                            + "incorreta e ausente rejeitados."
            );

        } finally {
            // Remover os arquivos temporarios
            try (var arquivos = Files.list(pasta)) {
                arquivos.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            }

            Files.deleteIfExists(pasta);
        }
    }

    private static SignedLicense montar(
            byte[] payload,
            byte[] assinatura,
            String keyId
    ) {

        return new SignedLicense(
                Base64.getEncoder()
                        .encodeToString(payload),

                Base64.getEncoder()
                        .encodeToString(assinatura),

                keyId
        );
    }

    private static byte[] assinar(
            PrivateKey chave,
            byte[] payload
    ) throws Exception {

        Signature assinatura =
                Signature.getInstance("Ed25519");

        assinatura.initSign(chave);
        assinatura.update(payload);

        return assinatura.sign();
    }

    private static void conferir(
            boolean condicao,
            String mensagem
    ) {

        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
