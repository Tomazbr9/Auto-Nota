
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
    @FXML private TableColumn<Produto, String> colUnidade;

    @FXML private TextField txtNome;
    @FXML private TextField txtPreco;
    @FXML private ComboBox<String> cbUnidade;

    private final ProdutoDAO dao = new ProdutoDAO();

    private Produto produtoSelecionado;

    @FXML
    public void initialize() {

        colNome.setCellValueFactory(
                new PropertyValueFactory<>("nome")
        );

        colPreco.setCellValueFactory(
                new PropertyValueFactory<>("preco")
        );

        colUnidade.setCellValueFactory(
                new PropertyValueFactory<>("unidade")
        );

        cbUnidade.setItems(
                FXCollections.observableArrayList("L", "UN")
        );

        cbUnidade.setValue("L");

        tabelaProdutos.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, antigo, selecionado) ->
                                preencherFormulario(selecionado)
                );

        carregarTabela();
    }

    private void carregarTabela() {

        try {
            tabelaProdutos.setItems(
                    FXCollections.observableArrayList(
                            dao.listarAtivos()
                    )
            );

        } catch (SQLException e) {
            mostrarAlerta(
                    "Erro",
                    "Falha ao carregar produtos: " + e.getMessage(),
                    Alert.AlertType.ERROR
            );
        }
    }

    private void preencherFormulario(Produto produto) {

        produtoSelecionado = produto;

        if (produto == null) {
            return;
        }

        txtNome.setText(produto.getNome());

        txtPreco.setText(
                String.format(
                        java.util.Locale.US,
                        "%.2f",
                        produto.getPreco()
                )
        );

        cbUnidade.setValue(produto.getUnidade());
    }

    @FXML
    private void novoProduto() {
        tabelaProdutos.getSelectionModel().clearSelection();
        produtoSelecionado = null;

        txtNome.clear();
        txtPreco.clear();
        cbUnidade.setValue("L");

        txtNome.requestFocus();
    }

    @FXML
    private void salvarProduto() {

        String nome = txtNome.getText().trim();
        String unidade = cbUnidade.getValue();

        if (nome.isBlank()) {
            mostrarAlerta(
                    "Validação",
                    "Informe o nome do produto.",
                    Alert.AlertType.WARNING
            );
            return;
        }

        if (unidade == null
                || (!unidade.equals("L")
                && !unidade.equals("UN"))) {

            mostrarAlerta(
                    "Validação",
                    "Selecione uma unidade válida.",
                    Alert.AlertType.WARNING
            );
            return;
        }

        double preco;

        try {
            preco = Double.parseDouble(
                    txtPreco.getText()
                            .trim()
                            .replace(",", ".")
            );

        } catch (NumberFormatException e) {
            mostrarAlerta(
                    "Validação",
                    "Informe um preço válido.",
                    Alert.AlertType.WARNING
            );
            return;
        }

        if (!Double.isFinite(preco) || preco <= 0) {
            mostrarAlerta(
                    "Validação",
                    "O preço deve ser maior que zero.",
                    Alert.AlertType.WARNING
            );
            return;
        }

        try {
            boolean novoCadastro =
                    produtoSelecionado == null;

            Produto produto = novoCadastro
                    ? new Produto()
                    : produtoSelecionado;

            produto.setNome(nome);
            produto.setPreco(preco);
            produto.setUnidade(unidade);

            if (novoCadastro) {
                dao.salvar(produto);
            } else {
                dao.atualizar(produto);
            }

            carregarTabela();
            novoProduto();

            mostrarAlerta(
                    "Sucesso",
                    "Produto salvo com sucesso!",
                    Alert.AlertType.INFORMATION
            );

        } catch (SQLException e) {
            mostrarAlerta(
                    "Erro",
                    "Não foi possível salvar: " + e.getMessage(),
                    Alert.AlertType.ERROR
            );
        }
    }

    private void mostrarAlerta(
            String titulo,
            String mensagem,
            Alert.AlertType tipo
    ) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
