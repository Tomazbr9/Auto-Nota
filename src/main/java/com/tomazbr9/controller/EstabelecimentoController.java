
package com.tomazbr9.controller;

import com.tomazbr9.dao.EstabelecimentoDAO;
import com.tomazbr9.model.Estabelecimento;
import com.tomazbr9.util.CnpjFormatter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EstabelecimentoController {

    @FXML private TextField txtNome;
    @FXML private TextField txtCnpj;
    @FXML private TextField txtEndereco;
    @FXML private TextField txtCidade;
    @FXML private TextField txtUf;
    @FXML private TextField txtCep;
    @FXML private TextField txtTelefone;

    private final EstabelecimentoDAO dao =
            new EstabelecimentoDAO();

    private Estabelecimento estabelecimento;


    @FXML
    public void initialize() {

        txtCnpj.setTextFormatter(
                new TextFormatter<String>(change -> {

                    String novoTexto = change.getControlNewText();

                    // Aceita digitos e pontuacao do CNPJ.
                    if (!novoTexto.matches("[0-9./\\-\\s]*")) {
                        return null;
                    }

                    String numeros =
                            CnpjFormatter.somenteNumeros(novoTexto);

                    // Limite de 14 digitos.
                    if (numeros.length() > 14) {
                        return null;
                    }

                    String formatado =
                            CnpjFormatter.formatarParcial(numeros);

                    // Substitui o texto pela versao formatada.
                    change.setRange(
                            0,
                            change.getControlText().length()
                    );

                    change.setText(formatado);

                    change.setCaretPosition(formatado.length());
                    change.setAnchor(formatado.length());

                    return change;
                })
        );

        carregarDados();
    }

    private void carregarDados() {
        try {
            dao.buscar().ifPresent(dados -> {
                estabelecimento = dados;

                txtNome.setText(dados.getNome());
                txtCnpj.setText(CnpjFormatter.formatar(dados.getCnpj()));
                txtEndereco.setText(dados.getEndereco());
                txtCidade.setText(dados.getCidade());
                txtUf.setText(dados.getUf());
                txtCep.setText(dados.getCep());
                txtTelefone.setText(dados.getTelefone());
            });

        } catch (SQLException e) {
            mostrarAlerta(
                    "Erro",
                    "Falha ao carregar configurações: "
                            + e.getMessage(),
                    Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void salvar() {

        String nome = txtNome.getText().trim();
        String cnpj = CnpjFormatter.somenteNumeros(
                txtCnpj.getText()
        );

        if (nome.isBlank() || cnpj.isBlank()) {
            mostrarAlerta(
                    "Validação",
                    "Nome e CNPJ são obrigatórios.",
                    Alert.AlertType.WARNING
            );
            return;
        }

        if (cnpj.length() != 14) {
            mostrarAlerta(
                    "Validação",
                    "O CNPJ deve conter 14 dígitos.",
                    Alert.AlertType.WARNING
            );
            return;
        }

        try {
            boolean novoCadastro =
                    estabelecimento == null;

            if (novoCadastro) {
                estabelecimento = new Estabelecimento();
            }

            estabelecimento.setNome(nome);
            estabelecimento.setCnpj(cnpj);

            estabelecimento.setEndereco(
                    txtEndereco.getText().trim()
            );

            estabelecimento.setCidade(
                    txtCidade.getText().trim()
            );

            estabelecimento.setUf(
                    txtUf.getText().trim().toUpperCase()
            );

            estabelecimento.setCep(
                    txtCep.getText().trim()
            );

            estabelecimento.setTelefone(
                    txtTelefone.getText().trim()
            );

            if (novoCadastro) {
                dao.salvar(estabelecimento);
            } else {
                dao.atualizar(estabelecimento);
            }

            mostrarAlerta(
                    "Sucesso",
                    "Configurações salvas com sucesso!",
                    Alert.AlertType.INFORMATION
            );

            fecharJanela();

        } catch (SQLException e) {
            mostrarAlerta(
                    "Erro",
                    "Falha ao salvar configurações: "
                            + e.getMessage(),
                    Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void cancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) txtNome
                .getScene().getWindow();

        stage.close();
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
