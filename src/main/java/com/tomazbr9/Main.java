package com.tomazbr9;

import com.tomazbr9.controller.AtivacaoController;

import com.tomazbr9.licensing.LicenseService;
import com.tomazbr9.licensing.LicenseValidationResult;

import com.tomazbr9.util.DatabaseInitializer;
import com.tomazbr9.util.DatabaseStorage;

import javafx.application.Application;
import javafx.application.Platform;

import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class Main extends Application {

    private LicenseService licenseService;

    @Override
    public void start(Stage primaryStage) {

        try {
            // 1. Inicializar licenciamento.

            licenseService =
                    LicenseService.criarPadrao();

            // 2. Verificar licenca instalada.

            LicenseValidationResult resultado =
                    licenseService.verificarInstalada();

            if (resultado.valid()) {

                abrirCaixa(primaryStage);

            } else {

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
    // ABRIR CAIXA
    // =====================================

    private void abrirCaixa(Stage stage)
            throws IOException, SQLException {

        // 1. Preparar pasta e migrar banco antigo.
        // Isso ocorre antes de qualquer conexao
        // com o novo banco.

        DatabaseStorage.preparar();

        // 2. Inicializar ou atualizar tabelas.

        DatabaseInitializer.inicializarBanco();

        // 3. Carregar a interface.

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
    // ABRIR ATIVACAO
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

                () -> {
                    try {
                        abrirCaixa(stage);

                    } catch (Exception e) {

                        mostrarErroFatal(
                                "Licenca instalada, mas nao foi "
                                        + "possivel abrir o caixa:\n"
                                        + e.getMessage()
                        );
                    }
                },

                motivo
        );

        stage.setTitle("Auto-Nota - Ativacao");

        stage.setScene(new Scene(root));

        stage.setResizable(false);
        stage.centerOnScreen();
    }

    private void mostrarErroFatal(String mensagem) {

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
