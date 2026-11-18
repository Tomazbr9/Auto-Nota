package com.tomazbr9.licensing;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import java.time.LocalDate;

import java.util.Base64;

public class MachineLicenseValidatorTest {

    public static void main(String[] args)
            throws Exception {

        // 1. Simular dois computadores

        String maquinaA = InstallationIdProvider.derivar(
                "LINUX",
                "0123456789abcdef0123456789abcdef"
        );

        String maquinaARepetida =
                InstallationIdProvider.derivar(
                        "LINUX",
                        "0123456789abcdef0123456789abcdef"
                );

        String maquinaB = InstallationIdProvider.derivar(
                "LINUX",
                "fedcba9876543210fedcba9876543210"
        );

        conferir(
                maquinaA.equals(maquinaARepetida),
                "O identificador deveria ser estavel."
        );

        conferir(
                !maquinaA.equals(maquinaB),
                "Maquinas diferentes geraram o mesmo codigo."
        );

        // 2. Criar um par temporario de chaves

        KeyPair chaves =
                KeyPairGenerator
                        .getInstance("Ed25519")
                        .generateKeyPair();

        // 3. Criar licenca para a maquina A

        License license = new License(
                License.CURRENT_SCHEMA_VERSION,
                "AN-TEST-MACHINE-01",
                License.PRODUCT_ID,
                "Posto Exemplo",
                maquinaA,
                LocalDate.of(2026, 11, 18),
                LicenseType.PERPETUAL
        );

        byte[] payload =
                LicensePayloadCodec.codificar(license);

        Signature assinador =
                Signature.getInstance("Ed25519");

        assinador.initSign(chaves.getPrivate());
        assinador.update(payload);

        byte[] assinatura = assinador.sign();

        // 4. Criar arquivo temporario

        Path pasta = Files.createTempDirectory(
                "auto-nota-machine-test-"
        );

        Path arquivoValido =
                pasta.resolve("valido.lic");

        Path arquivoAlterado =
                pasta.resolve("alterado.lic");

        try {
            LicenseFileStore.salvar(
                    arquivoValido,
                    criarLicencaAssinada(
                            payload,
                            assinatura
                    )
            );

            LicenseValidator base =
                    new LicenseValidator(
                            chaves.getPublic()
                    );

            MachineLicenseValidator validadorA =
                    new MachineLicenseValidator(
                            base,
                            maquinaA
                    );

            MachineLicenseValidator validadorB =
                    new MachineLicenseValidator(
                            base,
                            maquinaB
                    );

            // TESTE 1 - Computador autorizado

            LicenseValidationResult resultadoA =
                    validadorA.validar(arquivoValido);

            conferir(
                    resultadoA.valid(),
                    "A maquina autorizada deveria aceitar a licenca."
            );

            // TESTE 2 - Outro computador

            LicenseValidationResult resultadoB =
                    validadorB.validar(arquivoValido);

            conferir(
                    !resultadoB.valid(),
                    "Outra maquina nao pode usar a licenca."
            );

            // TESTE 3 - Conteudo adulterado

            byte[] payloadAlterado =
                    payload.clone();

            payloadAlterado[10] ^= 1;

            LicenseFileStore.salvar(
                    arquivoAlterado,
                    criarLicencaAssinada(
                            payloadAlterado,
                            assinatura
                    )
            );

            conferir(
                    !validadorA.validar(
                            arquivoAlterado
                    ).valid(),
                    "Licenca alterada nao pode ser aceita."
            );

            // TESTE 4 - Verificar maquina real

            String identificacaoReal =
                    InstallationIdProvider.obter();

            conferir(
                    identificacaoReal.startsWith("AN1-"),
                    "Formato da identificacao incorreto."
            );

            conferir(
                    identificacaoReal.equals(
                            InstallationIdProvider.obter()
                    ),
                    "Identificacao mudou entre as consultas."
            );

            System.out.println(
                    "OK: identificacao estavel, "
                            + "licenca vinculada e "
                            + "alteracoes rejeitadas."
            );

        } finally {

            Files.deleteIfExists(arquivoAlterado);
            Files.deleteIfExists(arquivoValido);
            Files.deleteIfExists(pasta);
        }
    }

    private static SignedLicense criarLicencaAssinada(
            byte[] payload,
            byte[] assinatura
    ) {

        return new SignedLicense(
                Base64.getEncoder()
                        .encodeToString(payload),

                Base64.getEncoder()
                        .encodeToString(assinatura),

                "AUTO_NOTA_KEY_01"
        );
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
