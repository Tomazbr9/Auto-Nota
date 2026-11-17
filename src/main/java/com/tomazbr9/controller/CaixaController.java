
package com.tomazbr9.controller;

import com.tomazbr9.dao.ProdutoDAO;
import com.tomazbr9.dao.VendaDAO;

import com.tomazbr9.model.ItemVenda;
import com.tomazbr9.model.Produto;
import com.tomazbr9.model.Venda;

import com.tomazbr9.util.PrinterUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

public class CaixaController {

    @FXML private ComboBox<Produto> cbProduto;

    @FXML private TextField txtQuantidade;
    @FXML private TextField txtValor;

    @FXML private TextField txtCpf;
    @FXML private TextField txtPlaca;
    @FXML private TextField txtKm;

    @FXML private TableView<ItemVenda> tabelaItens;

    @FXML private TableColumn<ItemVenda, String> colProduto;
    @FXML private TableColumn<ItemVenda, String> colQuantidade;
    @FXML private TableColumn<ItemVenda, String> colUnitario;
    @FXML private TableColumn<ItemVenda, String> colSubtotal;

    @FXML private Label lblTotalDestaque;
    @FXML private Button btnFinalizar;

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();

    private final ObservableList<ItemVenda> itensVenda =
            FXCollections.observableArrayList();

    private boolean atualizandoCampos = false;

    @FXML
    public void initialize() {

        configurarTabela();
        carregarProdutos();
        configurarCalculoAutomatico();
        atualizarTotal();
    }

    // ========================================
    // CONFIGURACAO DA TABELA
    // ========================================

    private void configurarTabela() {

        colProduto.setCellValueFactory(
                dados -> new SimpleStringProperty(
                        dados.getValue()
                                .getProduto().getNome()
                )
        );

        colQuantidade.setCellValueFactory(
                dados -> {

                    ItemVenda item = dados.getValue();

                    return new SimpleStringProperty(
                            formatarQuantidade(item)
                    );
                }
        );

        colUnitario.setCellValueFactory(
                dados -> new SimpleStringProperty(
                        formatarMoeda(
                                dados.getValue()
                                        .getValorUnitario()
                        )
                )
        );

        colSubtotal.setCellValueFactory(
                dados -> new SimpleStringProperty(
                        formatarMoeda(
                                dados.getValue()
                                        .getSubtotal()
                        )
                )
        );

        tabelaItens.setItems(itensVenda);
    }

    // ========================================
    // CARREGAR PRODUTOS
    // ========================================

    private void carregarProdutos() {

        try {
            cbProduto.setItems(
                    FXCollections.observableArrayList(
                            produtoDAO.listarAtivos()
                    )
            );

        } catch (SQLException e) {
            mostrarAlerta(
                    "Erro",
                    "Falha ao carregar produtos: "
                            + e.getMessage(),
                    Alert.AlertType.ERROR
            );
        }
    }

    // ========================================
    // CALCULO AUTOMATICO DO ITEM
    // ========================================

    private void configurarCalculoAutomatico() {

        cbProduto.valueProperty().addListener(
                (obs, antigo, novo) -> {

                    // Produtos por unidade utilizam
                    // apenas entrada pela quantidade.
                    boolean combustivel = novo != null
                            && "L".equals(novo.getUnidade());

                    txtValor.setEditable(combustivel);

                    atualizarValorPorQuantidade();
                }
        );

        txtQuantidade.textProperty().addListener(
                (obs, antigo, novo) -> {

                    if (atualizandoCampos
                            || !txtQuantidade.isFocused()) {
                        return;
                    }

                    atualizarValorPorQuantidade();
                }
        );

        txtValor.textProperty().addListener(
                (obs, antigo, novo) -> {

                    if (atualizandoCampos
                            || !txtValor.isFocused()) {
                        return;
                    }

                    atualizarQuantidadePorValor();
                }
        );
    }

    private void atualizarValorPorQuantidade() {

        Produto produto = cbProduto.getValue();

        if (produto == null
                || txtQuantidade.getText().isBlank()) {
            txtValor.clear();
            return;
        }

        try {
            BigDecimal quantidade =
                    lerNumero(txtQuantidade.getText());

            BigDecimal preco = BigDecimal.valueOf(
                    produto.getPreco()
            );

            BigDecimal subtotal = quantidade
                    .multiply(preco)
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );

            atualizandoCampos = true;

            txtValor.setText(
                    subtotal.toPlainString()
            );

        } catch (NumberFormatException e) {
            txtValor.clear();

        } finally {
            atualizandoCampos = false;
        }
    }

    private void atualizarQuantidadePorValor() {

        Produto produto = cbProduto.getValue();

        if (produto == null
                || !"L".equals(produto.getUnidade())
                || txtValor.getText().isBlank()) {
            return;
        }

        try {
            BigDecimal valor =
                    lerNumero(txtValor.getText());

            BigDecimal preco = BigDecimal.valueOf(
                    produto.getPreco()
            );

            if (preco.signum() <= 0) {
                return;
            }

            BigDecimal quantidade = valor.divide(
                    preco,
                    3,
                    RoundingMode.HALF_UP
            );

            atualizandoCampos = true;

            txtQuantidade.setText(
                    quantidade.toPlainString()
            );

        } catch (NumberFormatException e) {
            // A validacao definitiva ocorre
            // quando o usuario adiciona o item.

        } finally {
            atualizandoCampos = false;
        }
    }

    // ========================================
    // ADICIONAR ITEM
    // ========================================

    @FXML
    private void adicionarItem() {

        try {
            Produto produto = cbProduto.getValue();

            if (produto == null) {
                throw new IllegalArgumentException(
                        "Selecione um produto."
                );
            }

            BigDecimal quantidade;

            try {
                quantidade =
                        lerNumero(txtQuantidade.getText());

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Informe uma quantidade valida."
                );
            }

            if (quantidade.signum() <= 0
                    || quantidade.compareTo(
                    new BigDecimal("1000000")
            ) > 0) {
                throw new IllegalArgumentException(
                        "Quantidade deve ser positiva "
                                + "e no maximo 1000000."
                );
            }

            String unidade = produto.getUnidade();

            int casasDecimais = quantidade
                    .stripTrailingZeros()
                    .scale();

            if ("UN".equals(unidade)
                    && casasDecimais > 0) {

                throw new IllegalArgumentException(
                        "Produtos por unidade exigem "
                                + "quantidade inteira."
                );
            }

            if ("L".equals(unidade)
                    && casasDecimais > 3) {

                throw new IllegalArgumentException(
                        "Quantidade em litros aceita "
                                + "ate 3 casas decimais."
                );
            }

            if (!"L".equals(unidade)
                    && !"UN".equals(unidade)) {

                throw new IllegalArgumentException(
                        "Unidade do produto invalida."
                );
            }

            if (produto.getPreco() == null
                    || !Double.isFinite(
                    produto.getPreco()
            )
                    || produto.getPreco() <= 0) {

                throw new IllegalArgumentException(
                        "Preco do produto invalido."
                );
            }

            BigDecimal preco = BigDecimal.valueOf(
                    produto.getPreco()
            );

            BigDecimal subtotal = quantidade
                    .multiply(preco)
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );

            if (subtotal.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Subtotal do item deve ser positivo."
                );
            }

            ItemVenda item = new ItemVenda();

            item.setProduto(produto);
            item.setQuantidade(quantidade.doubleValue());
            item.setValorUnitario(produto.getPreco());
            item.setSubtotal(subtotal.doubleValue());

            itensVenda.add(item);

            atualizarTotal();

            txtQuantidade.clear();
            txtValor.clear();

            txtQuantidade.requestFocus();

        } catch (IllegalArgumentException e) {

            mostrarAlerta(
                    "Validacao",
                    e.getMessage(),
                    Alert.AlertType.WARNING
            );
        }
    }

    // ========================================
    // REMOVER ITEM
    // ========================================

    @FXML
    private void removerItem() {

        ItemVenda item = tabelaItens
                .getSelectionModel()
                .getSelectedItem();

        if (item == null) {
            mostrarAlerta(
                    "Aviso",
                    "Selecione um item para remover.",
                    Alert.AlertType.WARNING
            );
            return;
        }

        itensVenda.remove(item);

        atualizarTotal();
    }

    // ========================================
    // TOTAL DA VENDA
    // ========================================

    private BigDecimal calcularTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (ItemVenda item : itensVenda) {

            total = total.add(
                    BigDecimal.valueOf(
                            item.getSubtotal()
                    )
            );
        }

        return total.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private void atualizarTotal() {

        lblTotalDestaque.setText(
                formatarMoeda(
                        calcularTotal().doubleValue()
                )
        );
    }

    // ========================================
    // FINALIZAR VENDA
    // ========================================

    @FXML
    private void finalizarVenda() {

        if (itensVenda.isEmpty()) {

            mostrarAlerta(
                    "Aviso",
                    "Adicione pelo menos um produto.",
                    Alert.AlertType.WARNING
            );

            return;
        }

        try {
            Venda venda = new Venda();

            venda.setDataHora(LocalDateTime.now());

            venda.setCnpjCpf(
                    txtCpf.getText().trim()
            );

            venda.setPlaca(
                    txtPlaca.getText().trim()
            );

            if (!txtKm.getText().isBlank()) {

                int km;

                try {
                    km = Integer.parseInt(
                            txtKm.getText().trim()
                    );

                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Odometro invalido."
                    );
                }

                if (km < 0) {
                    throw new IllegalArgumentException(
                            "Odometro nao pode ser negativo."
                    );
                }

                venda.setKm(km);
            }

            venda.setTotal(
                    calcularTotal().doubleValue()
            );

            for (ItemVenda item : itensVenda) {
                venda.addItem(item);
            }

            // Grava uma unica venda com todos os itens.
            vendaDAO.registrarVenda(venda);

            try {
                PrinterUtil.imprimirCupom(venda);

                mostrarAlerta(
                        "Sucesso",
                        "Venda n. " + venda.getId()
                                + " registrada e impressa!",
                        Alert.AlertType.INFORMATION
                );

            } catch (Exception e) {

                mostrarAlerta(
                        "Aviso",
                        "Venda registrada, mas a impressao "
                                + "falhou:\n" + e.getMessage(),
                        Alert.AlertType.WARNING
                );
            }

            limparVenda();

        } catch (SQLException e) {

            mostrarAlerta(
                    "Erro",
                    "Falha ao registrar venda: "
                            + e.getMessage(),
                    Alert.AlertType.ERROR
            );

        } catch (IllegalArgumentException e) {

            mostrarAlerta(
                    "Validacao",
                    e.getMessage(),
                    Alert.AlertType.WARNING
            );
        }
    }

    private void limparVenda() {

        itensVenda.clear();

        txtQuantidade.clear();
        txtValor.clear();

        txtCpf.clear();
        txtPlaca.clear();
        txtKm.clear();

        atualizarTotal();

        cbProduto.requestFocus();
    }

    // ========================================
    // GERENCIAR PRODUTOS
    // ========================================

    @FXML
    private void abrirGerenciamentoProdutos() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/GerenciarProdutos.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle("Gerenciar Produtos");
            stage.setScene(new Scene(root, 550, 520));

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );

            stage.setResizable(false);
            stage.showAndWait();

            carregarProdutos();

        } catch (IOException e) {

            mostrarAlerta(
                    "Erro",
                    "Falha ao abrir produtos: "
                            + e.getMessage(),
                    Alert.AlertType.ERROR
            );
        }
    }

    // ========================================
    // CONFIGURACOES DO ESTABELECIMENTO
    // ========================================

    @FXML
    private void abrirConfiguracoes() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/Estabelecimento.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle(
                    "Configuracoes do Estabelecimento"
            );

            stage.setScene(new Scene(root));

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );

            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {

            mostrarAlerta(
                    "Erro",
                    "Falha ao abrir configuracoes: "
                            + e.getMessage(),
                    Alert.AlertType.ERROR
            );
        }
    }

    // ========================================
    // METODOS AUXILIARES
    // ========================================

    private BigDecimal lerNumero(String texto) {

        return new BigDecimal(
                texto.trim().replace(",", ".")
        );
    }

    private String formatarQuantidade(ItemVenda item) {

        String unidade =
                item.getProduto().getUnidade();

        if ("UN".equals(unidade)) {

            return String.format(
                    Locale.US,
                    "%.0f UN",
                    item.getQuantidade()
            );
        }

        return String.format(
                Locale.US,
                "%.3f L",
                item.getQuantidade()
        );
    }

    private String formatarMoeda(double valor) {

        return String.format(
                Locale.forLanguageTag("pt-BR"),
                "R$ %.2f",
                valor
        );
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
