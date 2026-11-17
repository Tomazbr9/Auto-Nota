package com.tomazbr9.controller;

import com.tomazbr9.dao.ProdutoDAO;
import com.tomazbr9.model.Produto;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;

public class ProdutoController {

    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, String> colNome;
    @FXML private TableColumn<Produto, Double> colPreco;
    @FXML private TextField txtNome;
    @FXML private TextField txtPreco;

    private ProdutoDAO dao = new ProdutoDAO();
    private Produto produtoSelecionado;

    @FXML
    public void initialize() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("preco"));
        carregarTabela();
    }

    private void carregarTabela() {
        try {
            tabelaProdutos.setItems(FXCollections.observableArrayList(dao.listarAtivos()));
        } catch (SQLException e) {
            mostrarAlerta("Erro", "Falha ao carregar produtos.");
        }
    }

    @FXML
    private void selecionarProduto() {
        produtoSelecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (produtoSelecionado != null) {
            txtNome.setText(produtoSelecionado.getNome());
            txtPreco.setText(String.format("%.2f", produtoSelecionado.getPreco()));
        }
    }

    @FXML
    private void atualizarPreco() {
        if (produtoSelecionado == null) return;

        try {
            double novoPreco = Double.parseDouble(txtPreco.getText().replace(",", "."));
            produtoSelecionado.setPreco(novoPreco);

            dao.atualizar(produtoSelecionado);
            carregarTabela();
            mostrarAlerta("Sucesso", "Preço atualizado com sucesso!");
        } catch (Exception e) {
            mostrarAlerta("Erro", "Valor inválido ou falha no banco.");
        }
    }

    private void mostrarAlerta(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setContentText(msg);
        alert.show();
    }
}