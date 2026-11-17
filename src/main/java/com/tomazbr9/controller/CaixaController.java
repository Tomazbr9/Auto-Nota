package com.tomazbr9.controller;

import com.tomazbr9.dao.ProdutoDAO;
import com.tomazbr9.dao.VendaDAO;
import com.tomazbr9.model.ItemVenda;
import com.tomazbr9.model.Produto;
import com.tomazbr9.model.Venda;
import com.tomazbr9.util.PrinterUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CaixaController {

    @FXML
    private ComboBox<Produto> cbProduto;
    @FXML private TextField txtLitros;
    @FXML private TextField txtValor;
    @FXML private TextField txtCpf;
    @FXML private TextField txtPlaca;
    @FXML private TextField txtKm;
    @FXML private Label lblTotalDestaque;
    @FXML private Button btnFinalizar;

    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private VendaDAO vendaDAO = new VendaDAO();

    // Variável para evitar loop infinito na atualização dos campos de texto
    private boolean isCalculando = false;

    @FXML
    public void initialize() {
        carregarProdutos();
        configurarCalculoAutomatico();
    }

    private void carregarProdutos() {
        try {
            // Busca no banco e joga direto no ComboBox
            cbProduto.setItems(FXCollections.observableArrayList(produtoDAO.listarAtivos()));
        } catch (SQLException e) {
            mostrarAlerta("Erro", "Falha ao carregar combustíveis: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void configurarCalculoAutomatico() {
        // Quando o usuário digita nos LITROS
        txtLitros.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isCalculando || !txtLitros.isFocused() || cbProduto.getValue() == null) return;
            try {
                isCalculando = true;
                double litros = newVal.isEmpty() ? 0 : Double.parseDouble(newVal.replace(",", "."));
                double precoStr = cbProduto.getValue().getPreco();
                double total = litros * precoStr;

                txtValor.setText(String.format("%.2f", total).replace(",", "."));
                atualizarTotalDestaque(total);
            } catch (NumberFormatException ignored) {
            } finally {
                isCalculando = false;
            }
        });

        // Quando o usuário digita no VALOR TOTAL
        txtValor.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isCalculando || !txtValor.isFocused() || cbProduto.getValue() == null) return;
            try {
                isCalculando = true;
                double valor = newVal.isEmpty() ? 0 : Double.parseDouble(newVal.replace(",", "."));
                double precoStr = cbProduto.getValue().getPreco();
                double litros = valor / precoStr;

                txtLitros.setText(String.format("%.3f", litros).replace(",", "."));
                atualizarTotalDestaque(valor);
            } catch (NumberFormatException ignored) {
            } finally {
                isCalculando = false;
            }
        });

        cbProduto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !txtLitros.getText().isEmpty()) {

                txtLitros.requestFocus();
                txtLitros.setText(txtLitros.getText() + " ");
                txtLitros.setText(txtLitros.getText().trim());
            }
        });
    }

    private void atualizarTotalDestaque(double total) {
        lblTotalDestaque.setText(String.format("R$ %.2f", total));
    }

    @FXML
    public void abrirGerenciamentoProdutos() {
        try {
            // Carrega o FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GerenciarProdutos.fxml"));

            // CORREÇÃO: Declare o 'root' AQUI DENTRO, não lá em cima na classe!
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Gerenciar Combustíveis");
            stage.setScene(new Scene(root, 400, 400)); // Define o root fresco aqui

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void finalizarVenda() {
        Produto produtoSelecionado = cbProduto.getValue();

        if (produtoSelecionado == null || txtLitros.getText().isEmpty() || txtValor.getText().isEmpty()) {
            mostrarAlerta("Aviso", "Selecione o produto e informe os valores.", Alert.AlertType.WARNING);
            return;
        }

        try {
            double litros = Double.parseDouble(txtLitros.getText());
            double total = Double.parseDouble(txtValor.getText());

            // 1. Monta o Cabeçalho da Venda
            Venda venda = new Venda();
            venda.setDataHora(LocalDateTime.now());
            venda.setTotal(total);
            venda.setCnpjCpf(txtCpf.getText().trim());
            venda.setPlaca(txtPlaca.getText().trim());

            if (!txtKm.getText().isEmpty()) {
                venda.setKm(Integer.parseInt(txtKm.getText().trim()));
            }

            // 2. Monta o Item da Venda
            ItemVenda item = new ItemVenda();
            item.setProduto(produtoSelecionado);
            item.setLitros(litros);
            item.setValorUnitario(produtoSelecionado.getPreco());
            item.setSubtotal(total);

            venda.addItem(item); // Adiciona o item na lista da venda

            // 3. Salva no Banco de Dados
            vendaDAO.registrarVenda(venda);

            // 4. Manda para a Impressora!
            try {
                PrinterUtil.imprimirCupom(venda);
                mostrarAlerta("Sucesso", "Venda nº " + venda.getId() + " registrada e impressa!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                // Se der erro na impressora (ex: desligada ou sem papel), avisa,
                // mas não perde a venda pois já salvou no banco.
                mostrarAlerta("Aviso", "Venda salva, mas falhou ao imprimir:\n" + e.getMessage(), Alert.AlertType.WARNING);
            }

            // 5. Sucesso! (Aqui entraremos com a impressão ESC/POS depois)
            mostrarAlerta("Sucesso", "Venda nº " + venda.getId() + " registrada com sucesso!\nPreparando impressão...", Alert.AlertType.INFORMATION);
            limparTela();

        } catch (NumberFormatException e) {
            mostrarAlerta("Erro", "Valores numéricos inválidos.", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            mostrarAlerta("Erro", "Falha ao gravar no banco: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void limparTela() {
        txtLitros.clear();
        txtValor.clear();
        txtCpf.clear();
        txtPlaca.clear();
        txtKm.clear();
        lblTotalDestaque.setText("R$ 0,00");
        cbProduto.requestFocus();
    }

    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
