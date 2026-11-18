package com.tomazbr9;

import com.tomazbr9.controller.AtivacaoController;

import com.tomazbr9.licensing.LicenseService;
import com.tomazbr9.licensing.LicenseValidationResult;

import com.tomazbr9.util.DatabaseInitializer;

import javafx.application.Application;
import javafx.application.Platform;

import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private LicenseService licenseService;

    @Override
    public void start(Stage primaryStage) {

        try {

            // 1. Inicializa o servico de licencas.
            // Se a chave publica estiver ausente,
            // o aplicativo nao libera o caixa.

            licenseService =
                    LicenseService.criarPadrao();

            // 2. Verifica se o aplicativo
            // ja possui uma licenca valida.

            LicenseValidationResult resultado =
                    licenseService.verificarInstalada();

            if (resultado.valid()) {

                // Licenca valida:
                // abre normalmente o caixa.

                abrirCaixa(primaryStage);

            } else {

                // Licenca ausente ou invalida:
                // mostra a tela de ativacao.

                abrirAtivacao(
                        primaryStage,
                        resultado.message()
                );
            }

            primaryStage.show();

        } catch (Exception e) {

            mostrarErroFatal(
                    "Nao foi possivel iniciar o Auto-Nota:\n"
                            + e.getMessage()
            );

            Platform.exit();
        }
    }

    // =====================================
    // ABRIR TELA DO CAIXA
    // =====================================

    private void abrirCaixa(Stage stage)
            throws IOException {

        // O banco e a tela do caixa somente
        // sao inicializados depois da ativacao.

        DatabaseInitializer.inicializarBanco();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/fxml/TelaCaixa.fxml"
                )
        );

        Parent root = loader.load();

        stage.setTitle(
                "Auto-Nota - Caixa de Abastecimento"
        );

        stage.setScene(new Scene(root));

        stage.setResizable(false);
        stage.centerOnScreen();
    }

    // =====================================
    // ABRIR TELA DE ATIVACAO
    // =====================================

    private void abrirAtivacao(
            Stage stage,
            String motivo
    ) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/fxml/Ativacao.fxml"
                )
        );

        Parent root = loader.load();

        AtivacaoController controller =
                loader.getController();

        controller.configurar(
                licenseService,

                // Executado apenas depois
                // da ativacao bem-sucedida.

                () -> {

                    try {
                        abrirCaixa(stage);

                    } catch (IOException e) {

                        mostrarErroFatal(
                                "Licenca instalada, mas nao "
                                        + "foi possivel abrir o caixa:\n"
                                        + e.getMessage()
                        );
                    }
                },

                motivo
        );

        stage.setTitle(
                "Auto-Nota - Ativacao"
        );

        stage.setScene(new Scene(root));

        stage.setResizable(false);
        stage.centerOnScreen();
    }

    private void mostrarErroFatal(
            String mensagem
    ) {

        Alert alerta = new Alert(
                Alert.AlertType.ERROR
        );

        alerta.setTitle("Erro no Auto-Nota");
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);

        alerta.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
