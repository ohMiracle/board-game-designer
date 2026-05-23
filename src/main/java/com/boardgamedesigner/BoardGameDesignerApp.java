package com.boardgamedesigner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BoardGameDesignerApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/main-view.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setTitle("Board Game Designer — 桌游卡牌排版工具");
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("无法加载主界面: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
