package com.tomazbr9.licensing.tools;

import com.tomazbr9.licensing.InstallationIdProvider;
import com.tomazbr9.licensing.LicenseValidationResult;
import com.tomazbr9.licensing.MachineLicenseValidator;

import java.nio.file.Path;

public final class VerifyMachineLicense {

    private VerifyMachineLicense() {
    }

    public static void main(String[] args)
            throws Exception {

        if (args.length != 1) {

            System.err.println(
                    "Uso: VerifyMachineLicense <arquivo.lic>"
            );

            System.exit(2);
        }

        String identificacao =
                InstallationIdProvider.obter();

        System.out.println(
                "Identificacao local: " + identificacao
        );

        MachineLicenseValidator validador =
                MachineLicenseValidator.criarPadrao();

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
                "LICENCA VALIDA PARA ESTE COMPUTADOR"
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
                "Tipo: "
                        + resultado.license().type()
        );
    }
}
