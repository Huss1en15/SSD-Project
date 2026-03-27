package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class main extends Application {
    // JavaFX application entry point - loads the login UI and shows the main stage
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

            Scene scene = new Scene(root, 350, 270);

            primaryStage.setTitle("Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Main method - launches the JavaFX application
    public static void main(String[] args) {
        launch(args);
    }
}