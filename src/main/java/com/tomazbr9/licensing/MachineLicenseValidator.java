package com.tomazbr9.licensing;

import java.io.IOException;
import java.nio.file.Path;

import java.security.GeneralSecurityException;
import java.util.Objects;

public final class MachineLicenseValidator {

    private final LicenseValidator licenseValidator;
    private final String installationIdAtual;

    public MachineLicenseValidator(
            LicenseValidator licenseValidator,
            String installationIdAtual
    ) {

        this.licenseValidator = Objects.requireNonNull(
                licenseValidator,
                "Validador obrigatorio."
        );

        if (installationIdAtual == null
                || installationIdAtual.isBlank()) {

            throw new IllegalArgumentException(
                    "Identificacao atual obrigatoria."
            );
        }

        this.installationIdAtual = installationIdAtual;
    }

    public static MachineLicenseValidator criarPadrao()
            throws IOException,
            GeneralSecurityException,
            InstallationIdException {

        return new MachineLicenseValidator(
                LicenseValidator.criarPadrao(),
                InstallationIdProvider.obter()
        );
    }

    public LicenseValidationResult validar(
            Path arquivo
    ) {

        // 1. Verifica a assinatura e os dados
        // usando o validador anterior.

        LicenseValidationResult resultado =
                licenseValidator.validar(arquivo);

        if (!resultado.valid()) {
            return resultado;
        }

        // 2. Compara a maquina atual
        // com a autorizada pela licenca.

        String installationIdLicenca =
                resultado.license().installationId();

        if (!installationIdAtual.equals(
                installationIdLicenca
        )) {

            return LicenseValidationResult.rejeitada(
                    "Esta licenca pertence "
                            + "a outro computador."
            );
        }

        // 3. Todos os criterios foram atendidos.

        return new LicenseValidationResult(
                true,
                "Licenca valida para este computador.",
                resultado.license()
        );
    }
}
