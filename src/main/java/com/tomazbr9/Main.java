package com.tomazbr9;

import com.tomazbr9.util.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Inicializa o banco de dados SQLite local
            DatabaseInitializer.inicializarBanco();

            // 2. Carrega o arquivo FXML da tela do caixa
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TelaCaixa.fxml"));
            Parent root = loader.load();

            // 3. Configura a Janela (Scene e Stage)
            Scene scene = new Scene(root);
            primaryStage.setTitle("PDV - Caixa de Abastecimento");
            primaryStage.setScene(scene);

            // Impede que o frentista desconfigure o layout esticando demais a janela
            primaryStage.setResizable(false);

            // 4. Exibe a tela
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Erro ao carregar o arquivo FXML da tela: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Método que dá o pontapé inicial no ciclo de vida do JavaFX
        launch(args);
    }
}