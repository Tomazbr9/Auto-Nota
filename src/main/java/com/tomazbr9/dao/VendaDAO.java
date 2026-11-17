package com.tomazbr9.dao;

import com.tomazbr9.model.ItemVenda;
import com.tomazbr9.model.Venda;
import com.tomazbr9.util.DatabaseConnection;

import java.sql.*;

public class VendaDAO {

    public void registrarVenda(Venda venda) throws SQLException {
        String sqlVenda = "INSERT INTO venda (cpf_cnpj, placa, km, total_venda) VALUES (?, ?, ?, ?)";
        String sqlItem = "INSERT INTO item_venda (venda_id, produto_id, litros, valor_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            // Desliga o salvamento automático para iniciar a Transação
            conn.setAutoCommit(false);

            // 1. Grava o Cabeçalho da Venda e pede o ID gerado de volta
            long vendaId = 0;
            try (PreparedStatement stmtVenda = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                stmtVenda.setString(1, venda.getCnpjCpf());
                stmtVenda.setString(2, venda.getPlaca());

                if (venda.getKm() != null) {
                    stmtVenda.setInt(3, venda.getKm());
                } else {
                    stmtVenda.setNull(3, Types.INTEGER);
                }

                stmtVenda.setDouble(4, venda.getTotal());
                stmtVenda.executeUpdate();

                // Recupera o ID gerado para esta venda
                try (ResultSet rs = stmtVenda.getGeneratedKeys()) {
                    if (rs.next()) {
                        vendaId = rs.getInt(1);
                        venda.setId(vendaId); // Atualiza o objeto Java com o ID do banco
                    } else {
                        throw new SQLException("Falha ao obter o ID da venda gerada.");
                    }
                }
            }

            // 2. Grava os Itens da Venda, amarrando ao ID da venda que acabamos de criar
            try (PreparedStatement stmtItem = conn.prepareStatement(sqlItem)) {
                for (ItemVenda item : venda.getItens()) {
                    stmtItem.setDouble(1, vendaId);
                    stmtItem.setDouble(2, item.getProduto().getId());
                    stmtItem.setDouble(3, item.getLitros());
                    stmtItem.setDouble(4, item.getValorUnitario());
                    stmtItem.setDouble(5, item.getSubtotal());

                    stmtItem.executeUpdate();
                }
            }

            // 3. Se chegou até aqui sem dar erro, confirma (salva) tudo de vez
            conn.commit();

        } catch (SQLException e) {
            // Se deu qualquer erro no meio do caminho, desfaz tudo que tentou salvar
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e; // Repassa o erro para a tela avisar o operador

        } finally {
            // Restaura o comportamento padrão da conexão
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }
}