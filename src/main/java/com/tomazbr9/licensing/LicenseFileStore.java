package com.tomazbr9.licensing;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.Properties;

public final class LicenseFileStore {

    private static final long MAX_FILE_BYTES =
            32 * 1024;

    private LicenseFileStore() {
    }

    public static void salvar(
            Path arquivo,
            SignedLicense license
    ) throws IOException {

        Path destino = arquivo
                .toAbsolutePath()
                .normalize();

        Path pasta = destino.getParent();

        Files.createDirectories(pasta);

        Properties properties = new Properties();

        properties.setProperty(
                "payloadBase64",
                license.payloadBase64()
        );

        properties.setProperty(
                "signatureBase64",
                license.signatureBase64()
        );

        properties.setProperty(
                "keyId",
                license.keyId()
        );

        // CREATE_NEW evita sobrescrever
        // licencas ja emitidas.

        try (Writer writer = Files.newBufferedWriter(
                destino,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        )) {

            properties.store(
                    writer,
                    "Auto-Nota License v1"
            );
        }
    }

    public static SignedLicense ler(Path arquivo)
            throws IOException {

        if (Files.size(arquivo) > MAX_FILE_BYTES) {
            throw new IOException(
                    "Arquivo de licenca muito grande."
            );
        }

        Properties properties = new Properties();

        try (Reader reader = Files.newBufferedReader(
                arquivo,
                StandardCharsets.UTF_8
        )) {

            properties.load(reader);
        }

        try {
            return new SignedLicense(
                    properties.getProperty(
                            "payloadBase64"
                    ),
                    properties.getProperty(
                            "signatureBase64"
                    ),
                    properties.getProperty(
                            "keyId"
                    )
            );

        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Arquivo de licenca incompleto.", e
            );
        }
    }
}
