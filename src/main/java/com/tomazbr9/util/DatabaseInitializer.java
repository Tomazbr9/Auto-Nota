package com.tomazbr9.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void inicializarBanco()
            throws SQLException {

        String tabelaProduto = """
                CREATE TABLE IF NOT EXISTS produto (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    preco REAL NOT NULL
                );
                """;

        String tabelaVenda = """
                CREATE TABLE IF NOT EXISTS venda (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    data_hora TEXT NOT NULL DEFAULT (
                        strftime('%Y-%m-%d %H:%M:%S',
                                 'now', 'localtime')
                    ),
                    cpf_cnpj TEXT,
                    placa TEXT,
                    km INTEGER,
                    bico INTEGER,
                    total_venda REAL NOT NULL
                        CHECK (total_venda >= 0)
                );
                """;

        String tabelaItemVenda = """
                CREATE TABLE IF NOT EXISTS item_venda (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venda_id INTEGER NOT NULL,
                    produto_id INTEGER NOT NULL,
                    quantidade REAL NOT NULL
                        CHECK (quantidade > 0),
                    valor_unitario REAL NOT NULL
                        CHECK (valor_unitario >= 0),
                    subtotal REAL NOT NULL
                        CHECK (subtotal >= 0),
                    FOREIGN KEY (venda_id)
                        REFERENCES venda(id) ON DELETE CASCADE,
                    FOREIGN KEY (produto_id)
                        REFERENCES produto(id)
                );
                """;

        String tabelaEstabelecimento = """
                CREATE TABLE IF NOT EXISTS estabelecimento (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    nome TEXT NOT NULL,
                    cnpj TEXT NOT NULL,
                    endereco TEXT,
                    cidade TEXT,
                    uf TEXT,
                    cep TEXT,
                    telefone TEXT
                );
                """;

        try (Connection conn =
                     DatabaseConnection.getConnection();

             Statement stmt = conn.createStatement()) {

            stmt.execute(tabelaProduto);
            stmt.execute(tabelaVenda);
            stmt.execute(tabelaItemVenda);
            stmt.execute(tabelaEstabelecimento);

            // Migracoes existentes.
            adicionarUnidadeProdutoSeNecessario(stmt);
            migrarQuantidadeItemVenda(stmt);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_venda_data
                    ON venda(data_hora)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_item_venda_venda
                    ON item_venda(venda_id)
                    """);

            // Preserva o comportamento atual:
            // produtos de exemplo apenas se
            // o catalogo estiver vazio.
            inserirProdutosIniciais(stmt);

            System.out.println(
                    "Banco SQLite inicializado: "
                            + AppPaths.banco()
            );
        }
    }

    private static void inserirProdutosIniciais(
            Statement stmt
    ) throws SQLException {

        boolean vazio;

        try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM produto"
        )) {
            vazio = rs.next() && rs.getInt(1) == 0;
        }

        if (vazio) {

            stmt.execute("""
                    INSERT INTO produto (nome, preco)
                    VALUES ('Gasolina Comum', 5.89)
                    """);

            stmt.execute("""
                    INSERT INTO produto (nome, preco)
                    VALUES ('Gasolina Aditivada', 6.09)
                    """);

            stmt.execute("""
                    INSERT INTO produto (nome, preco)
                    VALUES ('Etanol', 3.99)
                    """);
        }
    }

    private static void adicionarUnidadeProdutoSeNecessario(
            Statement stmt
    ) throws SQLException {

        boolean possuiUnidade = false;

        try (ResultSet rs = stmt.executeQuery(
                "PRAGMA table_info(produto)"
        )) {

            while (rs.next()) {

                if ("unidade".equalsIgnoreCase(
                        rs.getString("name")
                )) {
                    possuiUnidade = true;
                    break;
                }
            }
        }

        if (!possuiUnidade) {

            stmt.execute("""
                    ALTER TABLE produto
                    ADD COLUMN unidade TEXT NOT NULL
                    DEFAULT 'L'
                    CHECK (unidade IN ('L', 'UN'))
                    """);
        }
    }

    private static void migrarQuantidadeItemVenda(
            Statement stmt
    ) throws SQLException {

        boolean possuiLitros = false;
        boolean possuiQuantidade = false;

        try (ResultSet rs = stmt.executeQuery(
                "PRAGMA table_info(item_venda)"
        )) {

            while (rs.next()) {

                String coluna = rs.getString("name");

                if ("litros".equalsIgnoreCase(coluna)) {
                    possuiLitros = true;
                }

                if ("quantidade".equalsIgnoreCase(coluna)) {
                    possuiQuantidade = true;
                }
            }
        }

        if (possuiQuantidade) {
            return;
        }

        if (possuiLitros) {

            stmt.execute("""
                    ALTER TABLE item_venda
                    RENAME COLUMN litros TO quantidade
                    """);

        } else {

            throw new SQLException(
                    "Tabela item_venda sem coluna quantidade."
            );
        }
    }
}
