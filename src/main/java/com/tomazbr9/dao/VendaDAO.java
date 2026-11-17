package com.tomazbr9.dao;

import com.tomazbr9.model.ItemVenda;
import com.tomazbr9.model.Venda;
import com.tomazbr9.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.format.DateTimeFormatter;

public class VendaDAO {

    public void registrarVenda(Venda venda)
            throws SQLException {

        if (venda.getItens() == null
                || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException(
                    "A venda deve possuir ao menos um item."
            );
        }

        String sqlVenda = """
                INSERT INTO venda
                (data_hora, cpf_cnpj, placa, km, total_venda)
                VALUES (?, ?, ?, ?, ?)
                """;

        String sqlItem = """
                INSERT INTO item_venda
                (venda_id, produto_id, quantidade,
                 valor_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn =
                     DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                long vendaId;

                // 1. Registrar a venda

                try (PreparedStatement stmt =
                             conn.prepareStatement(sqlVenda)) {

                    String dataHora = venda.getDataHora()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "yyyy-MM-dd HH:mm:ss"
                                    )
                            );

                    stmt.setString(1, dataHora);
                    stmt.setString(2, venda.getCnpjCpf());
                    stmt.setString(3, venda.getPlaca());

                    if (venda.getKm() != null) {
                        stmt.setInt(4, venda.getKm());
                    } else {
                        stmt.setNull(
                                4, java.sql.Types.INTEGER
                        );
                    }

                    stmt.setDouble(5, venda.getTotal());

                    stmt.executeUpdate();
                }

                // 2. Recuperar o ID no SQLite

                try (PreparedStatement stmt =
                             conn.prepareStatement(
                                     "SELECT last_insert_rowid()"
                             );

                     ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                                "Falha ao recuperar o ID da venda."
                        );
                    }

                    vendaId = rs.getLong(1);

                    if (vendaId <= 0) {
                        throw new SQLException(
                                "ID da venda invalido."
                        );
                    }
                }

                // 3. Registrar todos os itens

                try (PreparedStatement stmt =
                             conn.prepareStatement(sqlItem)) {

                    for (ItemVenda item : venda.getItens()) {

                        stmt.setLong(1, vendaId);
                        stmt.setLong(
                                2,
                                item.getProduto().getId()
                        );

                        stmt.setDouble(
                                3,
                                item.getQuantidade()
                        );

                        stmt.setDouble(
                                4,
                                item.getValorUnitario()
                        );

                        stmt.setDouble(
                                5,
                                item.getSubtotal()
                        );

                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                }

                // 4. Confirmar a transacao

                conn.commit();

                venda.setId(vendaId);

            } catch (SQLException | RuntimeException e) {

                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;
            }
        }
    }
}
