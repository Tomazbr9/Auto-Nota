package com.tomazbr9.licensing;

public record SignedLicense(
        String payloadBase64,
        String signatureBase64,
        String keyId
) {

    public SignedLicense {

        if (payloadBase64 == null
                || payloadBase64.isBlank()) {

            throw new IllegalArgumentException(
                    "Conteudo da licenca obrigatorio."
            );
        }

        if (signatureBase64 == null
                || signatureBase64.isBlank()) {

            throw new IllegalArgumentException(
                    "Assinatura digital obrigatoria."
            );
        }

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Identificador da chave obrigatorio."
            );
        }
    }
}
