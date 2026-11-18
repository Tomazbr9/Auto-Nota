package com.tomazbr9.controller;

import com.tomazbr9.licensing.InstallationIdException;
import com.tomazbr9.licensing.InstallationIdProvider;
import com.tomazbr9.licensing.LicenseService;
import com.tomazbr9.licensing.LicenseValidationResult;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class AtivacaoController {

    @FXML
    private TextField txtIdentificacao;

    @FXML
    private Label lblStatus;

    private LicenseService licenseService;

    private Runnable quandoAtivada;

    public void configurar(
            LicenseService licenseService,
            Runnable quandoAtivada,
            String motivoInicial
    ) {

        this.licenseService = Objects.requireNonNull(
                licenseService
        );

        this.quandoAtivada = Objects.requireNonNull(
                quandoAtivada
        );

        lblStatus.setText(motivoInicial);

        // A identificacao e obtida aqui,
        // mas ainda nao e uma ativacao.

        try {

            String identificacao =
                    InstallationIdProvider.obter();

            txtIdentificacao.setText(
                    identificacao
            );

        } catch (InstallationIdException e) {

            txtIdentificacao.setText(
                    "Identificacao indisponivel"
            );

            lblStatus.setText(
                    "Nao foi possivel identificar o "
                            + "computador: " + e.getMessage()
            );
        }
    }

    // =====================================
    // COPIAR IDENTIFICACAO
    // =====================================

    @FXML
    private void copiarIdentificacao() {

        String identificacao =
                txtIdentificacao.getText();

        if (!identificacao.startsWith("AN1-")) {

            mostrarAlerta(
                    "Identificacao indisponivel",
                    "Nao foi possivel obter o codigo "
                            + "deste computador.",
                    Alert.AlertType.WARNING
            );

            return;
        }

        ClipboardContent conteudo =
                new ClipboardContent();

        conteudo.putString(identificacao);

        Clipboard.getSystemClipboard()
                .setContent(conteudo);

        lblStatus.setText(
                "Identificacao copiada. "
                        + "Envie o codigo ao fornecedor."
        );
    }

    // =====================================
    // SELECIONAR E ATIVAR LICENCA
    // =====================================

    @FXML
    private void selecionarLicenca() {

        FileChooser seletor = new FileChooser();

        seletor.setTitle(
                "Selecionar licenca do Auto-Nota"
        );

        seletor.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Licencas Auto-Nota (*.lic)",
                        "*.lic"
                )
        );

        Window janela =
                txtIdentificacao.getScene().getWindow();

        File arquivo = seletor.showOpenDialog(janela);

        // Cancelamento nao e erro.
        if (arquivo == null) {
            return;
        }

        try {

            LicenseValidationResult resultado =
                    licenseService.ativar(
                            arquivo.toPath()
                    );

            if (!resultado.valid()) {

                lblStatus.setText(
                        resultado.message()
                );

                mostrarAlerta(
                        "Licenca rejeitada",
                        resultado.message(),
                        Alert.AlertType.WARNING
                );

                return;
            }

            lblStatus.setText(
                    "Auto-Nota ativado com sucesso!"
            );

            mostrarAlerta(
                    "Ativacao concluida",
                    "Licenca vitalicia ativada para "
                            + "este computador.\n"
                            + "Cliente: "
                            + resultado.license()
                            .customerName(),
                    Alert.AlertType.INFORMATION
            );

            // Abre o caixa somente depois de
            // validar e instalar a licenca.
            quandoAtivada.run();

        } catch (IOException | SecurityException e) {

            lblStatus.setText(
                    "Falha ao salvar a licenca."
            );

            mostrarAlerta(
                    "Erro de ativacao",
                    "Nao foi possivel instalar a licenca:\n"
                            + e.getMessage(),
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
