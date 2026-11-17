package com.tomazbr9.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Caminho para o ficheiro local do banco de dados SQLite
    private static final String URL = "jdbc:sqlite:auto_nota.db";
    private static Connection conexao = null;

    public static Connection getConnection() throws SQLException {
        try {
            if (conexao == null || conexao.isClosed()) {

                // CORREÇÃO: O nome exato da classe do driver é org.sqlite.JDBC
                Class.forName("org.sqlite.JDBC");

                conexao = DriverManager.getConnection(URL);

                // Ativa o suporte a chaves estrangeiras por padrão na conexão
                try (var stmt = conexao.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC do SQLite não encontrado.", e);
        }
        return conexao;
    }
}