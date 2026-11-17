
package com.tomazbr9.dao;

import com.tomazbr9.model.Estabelecimento;
import com.tomazbr9.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class EstabelecimentoDAO {

    private static final long ESTABELECIMENTO_ID = 1L;

    public Optional<Estabelecimento> buscar() throws SQLException {

        String sql = """
                SELECT id, nome, cnpj, endereco,
                       cidade, uf, cep, telefone
                FROM estabelecimento
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ESTABELECIMENTO_ID);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }

        return Optional.empty();
    }

    public void salvar(Estabelecimento estabelecimento)
            throws SQLException {

        String sql = """
                INSERT INTO estabelecimento
                (id, nome, cnpj, endereco, cidade, uf, cep, telefone)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ESTABELECIMENTO_ID);
            preencherParametros(stmt, estabelecimento, 2);

            stmt.executeUpdate();
            estabelecimento.setId(ESTABELECIMENTO_ID);
        }
    }

    public void atualizar(Estabelecimento estabelecimento)
            throws SQLException {

        String sql = """
                UPDATE estabelecimento
                SET nome = ?,
                    cnpj = ?,
                    endereco = ?,
                    cidade = ?,
                    uf = ?,
                    cep = ?,
                    telefone = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            preencherParametros(stmt, estabelecimento, 1);
            stmt.setLong(8, ESTABELECIMENTO_ID);

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new SQLException(
                        "Nenhum estabelecimento cadastrado para atualizar."
                );
            }
        }
    }

    private void preencherParametros(
            PreparedStatement stmt,
            Estabelecimento estabelecimento,
            int inicio
    ) throws SQLException {

        stmt.setString(inicio, estabelecimento.getNome());
        stmt.setString(inicio + 1, estabelecimento.getCnpj());
        stmt.setString(inicio + 2, estabelecimento.getEndereco());
        stmt.setString(inicio + 3, estabelecimento.getCidade());
        stmt.setString(inicio + 4, estabelecimento.getUf());
        stmt.setString(inicio + 5, estabelecimento.getCep());
        stmt.setString(inicio + 6, estabelecimento.getTelefone());
    }

    private Estabelecimento mapear(ResultSet rs)
            throws SQLException {

        Estabelecimento estabelecimento = new Estabelecimento();

        estabelecimento.setId(rs.getLong("id"));
        estabelecimento.setNome(rs.getString("nome"));
        estabelecimento.setCnpj(rs.getString("cnpj"));
        estabelecimento.setEndereco(rs.getString("endereco"));
        estabelecimento.setCidade(rs.getString("cidade"));
        estabelecimento.setUf(rs.getString("uf"));
        estabelecimento.setCep(rs.getString("cep"));
        estabelecimento.setTelefone(rs.getString("telefone"));

        return estabelecimento;
    }
}
