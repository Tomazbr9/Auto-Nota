package com.tomazbr9.licensing;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;

import java.security.spec.X509EncodedKeySpec;

public final class LicensePublicKeyLoader {

    private LicensePublicKeyLoader() {
    }

    public static PublicKey carregar()
            throws IOException, GeneralSecurityException {

        try (InputStream input =
                     LicensePublicKeyLoader.class
                             .getResourceAsStream(
                                     "/licensing/auto-nota-public-key.der"
                             )) {

            if (input == null) {
                throw new IOException(
                        "Chave publica nao encontrada."
                );
            }

            byte[] bytes = input.readAllBytes();

            KeyFactory factory =
                    KeyFactory.getInstance("Ed25519");

            X509EncodedKeySpec spec =
                    new X509EncodedKeySpec(bytes);

            return factory.generatePublic(spec);
        }
    }
}
