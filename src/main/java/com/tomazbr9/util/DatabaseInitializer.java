package com.tomazbr9.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void inicializarBanco() {

        String tabelaProduto = "CREATE TABLE IF NOT EXISTS produto ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nome TEXT NOT NULL, "
                + "preco REAL NOT NULL"
                + ");";

        String tabelaVenda = "CREATE TABLE IF NOT EXISTS venda ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "data_hora TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')), "
                + "cpf_cnpj TEXT, "
                + "placa TEXT, "
                + "km INTEGER, "
                + "bico INTEGER,"
                + "total_venda REAL NOT NULL CHECK (total_venda >= 0)"
                + ");";

        String tabelaItemVenda = "CREATE TABLE IF NOT EXISTS item_venda ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "venda_id INTEGER NOT NULL, "
                + "produto_id INTEGER NOT NULL, "
                + "litros REAL NOT NULL CHECK (litros > 0), "
                + "valor_unitario REAL NOT NULL CHECK (valor_unitario >= 0), "
                + "subtotal REAL NOT NULL CHECK (subtotal >= 0), "
                + "FOREIGN KEY (venda_id) REFERENCES venda(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (produto_id) REFERENCES produto(id)"
                + ");";


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


        // Criação de índices para ganho de performance em consultas
        String indexVendas = "CREATE INDEX IF NOT EXISTS idx_venda_data ON venda(data_hora);";
        String indexItens = "CREATE INDEX IF NOT EXISTS idx_item_venda_venda ON item_venda(venda_id);";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Executa a criação de cada tabela e índice
            stmt.execute(tabelaProduto);
            stmt.execute(tabelaVenda);
            stmt.execute(tabelaItemVenda);
            stmt.execute(tabelaEstabelecimento);

            stmt.execute(indexVendas);
            stmt.execute(indexItens);


            // Inserir dados de teste caso a tabela de produtos esteja vazia
            inserirProdutosIniciais(stmt);

            System.out.println("Banco de dados SQLite inicializado com sucesso.");

        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados: " + e.getMessage());
        }
    }

    private static void inserirProdutosIniciais(Statement stmt) throws SQLException {
        // Verifica se já existem produtos cadastrados
        var rs = stmt.executeQuery("SELECT COUNT(*) FROM produto;");
        if (rs.next() && rs.getInt(1) == 0) {
            stmt.execute("INSERT INTO produto (nome, preco) VALUES ('Gasolina Comum', 5.89);");
            stmt.execute("INSERT INTO produto (nome, preco) VALUES ('Gasolina Aditivada', 6.09);");
            stmt.execute("INSERT INTO produto (nome, preco) VALUES ('Etanol', 3.99);");
            System.out.println("Combustíveis padrão inseridos na primeira execução.");
        }
    }
}