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
        String sql = "INSERT INTO produto (nome, preco) VALUES (?, ?)";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ){
            stmt.setString(1, produto.getNome());
            stmt.setDouble(2, produto.getPreco());

            stmt.executeUpdate();
        }
    }

    public void atualizar(Produto produto) throws SQLException {
        // CORREÇÃO: Nome da coluna de preço ajustado para 'preco_litro'
        String sql = "UPDATE produto SET nome = ?, preco = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produto.getNome());
            stmt.setDouble(2, produto.getPreco());

            // CORREÇÃO: O índice do ID é 3, pois é o terceiro '?' na string sql
            stmt.setLong(3, produto.getId());

            stmt.executeUpdate();
        }
    }

    public List<Produto> listarAtivos() throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM produto ORDER BY nome";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produto p = new Produto();
                p.setId(rs.getLong("id"));

                // CORREÇÃO: Busca os valores usando o nome real das colunas no banco de dados
                p.setNome(rs.getString("nome"));
                p.setPreco(rs.getDouble("preco"));

                produtos.add(p);
            }
        }
        return produtos;
    }
}