package com.tomazbr9.util;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection()
            throws SQLException {

        Path banco = AppPaths.banco();

        try {
            Files.createDirectories(
                    banco.getParent()
            );

        } catch (IOException e) {
            throw new SQLException(
                    "Sem acesso a pasta de dados: "
                            + banco.getParent(),
                    e
            );
        }

        String url = "jdbc:sqlite:"
                + banco.toAbsolutePath();

        Connection connection =
                DriverManager.getConnection(url);

        try (var stmt = connection.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            // Aguarda brevemente em casos de
            // bloqueio temporario do SQLite.
            stmt.execute("PRAGMA busy_timeout = 5000");

        } catch (SQLException e) {

            connection.close();
            throw e;
        }

        return connection;
    }
}
