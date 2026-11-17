package com.tomazbr9.licensing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

public final class LicensePayloadCodec {

    private static final int MAGIC = 0x414E4C31; // ANL1
    private static final int MAX_BYTES = 16 * 1024;

    private LicensePayloadCodec() {
    }

    public static byte[] codificar(License license) {
        Objects.requireNonNull(
                license, "Licenca obrigatoria"
        );

        try {
            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            try (DataOutputStream out =
                         new DataOutputStream(buffer)) {

                out.writeInt(MAGIC);
                out.writeInt(license.schemaVersion());

                out.writeUTF(license.licenseId());
                out.writeUTF(license.productId());
                out.writeUTF(license.customerName());
                out.writeUTF(license.installationId());

                out.writeLong(
                        license.issuedAt().toEpochDay()
                );

                out.writeUTF(license.type().name());
            }

            byte[] payload = buffer.toByteArray();

            if (payload.length > MAX_BYTES) {
                throw new IllegalArgumentException(
                        "Licenca muito grande."
                );
            }

            return payload;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Falha ao serializar licenca.", e
            );
        }
    }

    public static License decodificar(byte[] payload)
            throws IOException {

        if (payload == null
                || payload.length == 0
                || payload.length > MAX_BYTES) {

            throw new IOException(
                    "Conteudo da licenca ausente ou excessivo."
            );
        }

        try (DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(payload))) {

            if (in.readInt() != MAGIC) {
                throw new IOException(
                        "Formato de licenca nao reconhecido."
                );
            }

            int versao = in.readInt();

            String id = in.readUTF();
            String produto = in.readUTF();
            String cliente = in.readUTF();
            String instalacao = in.readUTF();

            LocalDate emissao = LocalDate.ofEpochDay(
                    in.readLong()
            );

            LicenseType tipo = LicenseType.valueOf(
                    in.readUTF()
            );

            if (in.available() != 0) {
                throw new IOException(
                        "Dados extras no conteudo da licenca."
                );
            }

            return new License(
                    versao,
                    id,
                    produto,
                    cliente,
                    instalacao,
                    emissao,
                    tipo
            );

        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Campos invalidos na licenca.", e
            );
        }
    }
}
