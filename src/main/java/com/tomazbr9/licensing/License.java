package com.tomazbr9.licensing;

import java.time.LocalDate;

public record License(
        int schemaVersion,
        String licenseId,
        String productId,
        String customerName,
        String installationId,
        LocalDate issuedAt,
        LicenseType type
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final String PRODUCT_ID = "AUTO_NOTA";

    public License {

        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Versao de licenca nao suportada."
            );
        }

        if (licenseId == null || licenseId.isBlank()) {
            throw new IllegalArgumentException(
                    "Identificador da licenca obrigatorio."
            );
        }

        if (!PRODUCT_ID.equals(productId)) {
            throw new IllegalArgumentException(
                    "Produto da licenca invalido."
            );
        }

        if (customerName == null
                || customerName.isBlank()) {

            throw new IllegalArgumentException(
                    "Nome do cliente obrigatorio."
            );
        }

        if (installationId == null
                || installationId.isBlank()) {

            throw new IllegalArgumentException(
                    "Identificador da instalacao obrigatorio."
            );
        }

        if (issuedAt == null) {
            throw new IllegalArgumentException(
                    "Data de emissao obrigatoria."
            );
        }

        if (type != LicenseType.PERPETUAL) {
            throw new IllegalArgumentException(
                    "Tipo de licenca nao suportado."
            );
        }
    }
}
