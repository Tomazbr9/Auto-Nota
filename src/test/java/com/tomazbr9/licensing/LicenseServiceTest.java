package com.tomazbr9.licensing;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;

import java.time.LocalDate;

import java.util.Base64;
import java.util.Comparator;

public class LicenseServiceTest {

    public static void main(String[] args)
            throws Exception {

        KeyPair chaves = KeyPairGenerator
                .getInstance("Ed25519")
                .generateKeyPair();

        String maquinaAutorizada =
                "AN1-11111111-22222222-33333333-44444444";

        Path pasta = Files.createTempDirectory(
                "auto-nota-activation-test-"
        );

        try {

            Path instalada = pasta.resolve(
                    "instalacao/license.lic"
            );

            Path origem = pasta.resolve(
                    "licenca-valida.lic"
            );

            LicenseValidator validator =
                    new LicenseValidator(
                            chaves.getPublic()
                    );

            LicenseService service =
                    new LicenseService(
                            validator,
                            () -> maquinaAutorizada,
                            instalada
                    );

            // TESTE 1: sem licenca instalada

            conferir(
                    !service.verificarInstalada().valid(),
                    "Sem licenca deveria ser rejeitado."
            );

            // TESTE 2: emitir licenca valida

            License licenca = new License(
                    License.CURRENT_SCHEMA_VERSION,
                    "AN-TEST-0001",
                    License.PRODUCT_ID,
                    "Posto Exemplo",
                    maquinaAutorizada,
                    LocalDate.now(),
                    LicenseType.PERPETUAL
            );

            criarArquivoAssinado(
                    origem,
                    licenca,
                    chaves.getPrivate()
            );

            // TESTE 3: ativar computador

            LicenseValidationResult ativacao =
                    service.ativar(origem);

            conferir(
                    ativacao.valid(),
                    "Licenca valida nao foi ativada."
            );

            conferir(
                    Files.isRegularFile(instalada),
                    "Arquivo nao foi instalado."
            );

            conferir(
                    service.verificarInstalada().valid(),
                    "Licenca instalada nao foi aceita."
            );

            // TESTE 4: rejeitar outra maquina

            Path outraLicenca = pasta.resolve(
                    "outra-maquina.lic"
            );

            License licencaOutraMaquina = new License(
                    License.CURRENT_SCHEMA_VERSION,
                    "AN-TEST-0002",
                    License.PRODUCT_ID,
                    "Posto Exemplo",
                    "AN1-AAAAAAAA-BBBBBBBB-CCCCCCCC-DDDDDDDD",
                    LocalDate.now(),
                    LicenseType.PERPETUAL
            );

            criarArquivoAssinado(
                    outraLicenca,
                    licencaOutraMaquina,
                    chaves.getPrivate()
            );

            conferir(
                    !service.ativar(outraLicenca).valid(),
                    "Licenca de outra maquina foi aceita."
            );

            // A licenca anterior deve continuar valida.

            conferir(
                    service.verificarInstalada().valid(),
                    "Licenca valida foi substituida."
            );

            // TESTE 5: rejeitar arquivo adulterado

            Path adulterada = pasta.resolve(
                    "adulterada.lic"
            );

            byte[] payload =
                    LicensePayloadCodec.codificar(licenca);

            byte[] assinatura =
                    assinar(chaves.getPrivate(), payload);

            payload[10] ^= 1;

            LicenseFileStore.salvar(
                    adulterada,
                    new SignedLicense(
                            Base64.getEncoder()
                                    .encodeToString(payload),
                            Base64.getEncoder()
                                    .encodeToString(assinatura),
                            "AUTO_NOTA_KEY_01"
                    )
            );

            conferir(
                    !service.ativar(adulterada).valid(),
                    "Arquivo adulterado foi aceito."
            );

            conferir(
                    service.verificarInstalada().valid(),
                    "Arquivo adulterado afetou a instalacao."
            );

            // TESTE 6: mesma licenca em outra maquina

            LicenseService outroComputador =
                    new LicenseService(
                            validator,
                            () -> "AN1-OUTRO-COMPUTADOR",
                            instalada
                    );

            conferir(
                    !outroComputador
                            .verificarInstalada().valid(),
                    "Outro computador aceitou a licenca."
            );

            System.out.println(
                    "OK: ativacao, persistencia, "
                            + "vinculacao e rejeicao "
                            + "de adulteracoes."
            );

        } finally {

            try (var caminhos = Files.walk(pasta)) {

                for (Path caminho : caminhos
                        .sorted(
                                Comparator.reverseOrder()
                        )
                        .toList()) {

                    Files.deleteIfExists(caminho);
                }
            }
        }
    }

    private static void criarArquivoAssinado(
            Path arquivo,
            License license,
            PrivateKey chavePrivada
    ) throws Exception {

        byte[] payload =
                LicensePayloadCodec.codificar(license);

        byte[] assinatura =
                assinar(chavePrivada, payload);

        SignedLicense assinada = new SignedLicense(
                Base64.getEncoder()
                        .encodeToString(payload),
                Base64.getEncoder()
                        .encodeToString(assinatura),
                "AUTO_NOTA_KEY_01"
        );

        LicenseFileStore.salvar(
                arquivo,
                assinada
        );
    }

    private static byte[] assinar(
            PrivateKey chave,
            byte[] conteudo
    ) throws Exception {

        Signature signature =
                Signature.getInstance("Ed25519");

        signature.initSign(chave);
        signature.update(conteudo);

        return signature.sign();
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
