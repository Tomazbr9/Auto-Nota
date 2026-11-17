
package com.tomazbr9.dao;

import com.tomazbr9.model.Produto;
import com.tomazbr9.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    public void salvar(Produto produto) throws SQLException {

        String sql = """
                INSERT INTO produto (nome, preco, unidade)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produto.getNome());
            stmt.setDouble(2, produto.getPreco());
            stmt.setString(3, produto.getUnidade());

            stmt.executeUpdate();
        }
    }

    public void atualizar(Produto produto) throws SQLException {

        String sql = """
                UPDATE produto
                SET nome = ?, preco = ?, unidade = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produto.getNome());
            stmt.setDouble(2, produto.getPreco());
            stmt.setString(3, produto.getUnidade());
            stmt.setLong(4, produto.getId());

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new SQLException(
                        "Produto não encontrado."
                );
            }
        }
    }

    public List<Produto> listarAtivos() throws SQLException {

        List<Produto> produtos = new ArrayList<>();

        String sql = """
                SELECT id, nome, preco, unidade
                FROM produto
                ORDER BY nome
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Produto produto = new Produto();

                produto.setId(rs.getLong("id"));
                produto.setNome(rs.getString("nome"));
                produto.setPreco(rs.getDouble("preco"));
                produto.setUnidade(rs.getString("unidade"));

                produtos.add(produto);
            }
        }

        return produtos;
    }
}
