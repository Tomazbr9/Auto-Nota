package com.tomazbr9.licensing;

public record LicenseValidationResult(
        boolean valid,
        String message,
        License license
) {

    public LicenseValidationResult {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Mensagem de validacao obrigatoria."
            );
        }

        if (valid && license == null) {
            throw new IllegalArgumentException(
                    "Uma licenca valida precisa conter seus dados."
            );
        }

        if (!valid && license != null) {
            throw new IllegalArgumentException(
                    "Uma licenca invalida nao pode ser autorizada."
            );
        }
    }

    public static LicenseValidationResult aprovada(
            License license
    ) {
        return new LicenseValidationResult(
                true,
                "Licenca autentica e com dados validos.",
                license
        );
    }

    public static LicenseValidationResult rejeitada(
            String motivo
    ) {
        return new LicenseValidationResult(
                false,
                motivo,
                null
        );
    }
}
