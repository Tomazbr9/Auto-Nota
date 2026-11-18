package com.tomazbr9.util;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.UUID;

public final class DatabaseStorage {

    private DatabaseStorage() {
    }

    public static void preparar() throws SQLException {

        Path destino = AppPaths.banco();

        try {
            Files.createDirectories(destino.getParent());

        } catch (IOException e) {
            throw new SQLException(
                    "Nao foi possivel criar a pasta de dados: "
                            + destino.getParent(),
                    e
            );
        }

        // O novo banco ja existe.
        // Nao sobrescrever nem importar novamente.
        if (Files.exists(destino)) {
            return;
        }

        // Caminho usado pelas versoes antigas.
        Path origem = Path.of("auto_nota.db")
                .toAbsolutePath()
                .normalize();

        if (origem.equals(destino)
                || !Files.isRegularFile(origem)) {

            // Primeira instalacao: o banco
            // sera criado normalmente.
            return;
        }

        // Criar um caminho temporario na
        // mesma pasta do novo banco.
        Path temporario = destino.getParent().resolve(
                "auto-nota-migracao-"
                        + UUID.randomUUID()
                        + ".db"
        );

        try {
            // 1. Gerar backup consistente.
            String urlOrigem = "jdbc:sqlite:"
                    + origem;

            try (Connection conn =
                         DriverManager.getConnection(urlOrigem);

                 PreparedStatement stmt =
                         conn.prepareStatement(
                                 "VACUUM INTO ?"
                         )) {

                stmt.setString(
                        1,
                        temporario.toString()
                );

                stmt.execute();
            }

            // 2. Conferir integridade.
            verificarIntegridade(temporario);

            // 3. Instalar o banco migrado.
            // Sem REPLACE_EXISTING.
            Files.move(temporario, destino);

            System.out.println(
                    "Banco migrado para: " + destino
            );

            System.out.println(
                    "Banco original preservado em: " + origem
            );

        } catch (IOException e) {

            throw new SQLException(
                    "Falha ao migrar o banco SQLite.",
                    e
            );

        } finally {

            try {
                Files.deleteIfExists(temporario);

            } catch (IOException e) {
                System.err.println(
                        "Falha ao remover arquivo temporario: "
                                + temporario
                );
            }
        }
    }

    private static void verificarIntegridade(
            Path banco
    ) throws SQLException {

        String url = "jdbc:sqlite:" + banco;

        try (Connection conn =
                     DriverManager.getConnection(url);

             Statement stmt = conn.createStatement();

             ResultSet rs = stmt.executeQuery(
                     "PRAGMA integrity_check"
             )) {

            if (!rs.next()
                    || !"ok".equalsIgnoreCase(
                    rs.getString(1)
            )
                    || rs.next()) {

                throw new SQLException(
                        "Banco migrado nao passou na "
                                + "verificacao de integridade."
                );
            }
        }
    }
}
