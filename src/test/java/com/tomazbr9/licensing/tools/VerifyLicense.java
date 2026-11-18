package com.tomazbr9.licensing.tools;

import com.tomazbr9.licensing.LicenseValidationResult;
import com.tomazbr9.licensing.LicenseValidator;

import java.nio.file.Path;

public final class VerifyLicense {

    private VerifyLicense() {
    }

    public static void main(String[] args)
            throws Exception {

        if (args.length != 1) {

            System.err.println(
                    "Uso: VerifyLicense <caminho-do-arquivo.lic>"
            );

            System.exit(2);
        }

        LicenseValidator validador =
                LicenseValidator.criarPadrao();

        LicenseValidationResult resultado =
                validador.validar(Path.of(args[0]));

        if (!resultado.valid()) {

            System.err.println(
                    "LICENCA REJEITADA: "
                            + resultado.message()
            );

            System.exit(1);
        }

        System.out.println(
                "ASSINATURA E DADOS VALIDOS"
        );

        System.out.println(
                "Licenca: "
                        + resultado.license().licenseId()
        );

        System.out.println(
                "Cliente: "
                        + resultado.license().customerName()
        );

        System.out.println(
                "Instalacao autorizada: "
                        + resultado.license().installationId()
        );

        System.out.println(
                "Modalidade: "
                        + resultado.license().type()
        );

        System.out.println(
                "Nota: ainda nao foi conferida a "
                        + "identidade do computador atual."
        );
    }
}
