package org.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        PackReadyView view = new PackReadyView();
        new PackReadyController(view);
        Scene scene = new Scene(view, 1120, 820);
        stage.setTitle("PackReady | School kit planner");
        stage.setMinWidth(420);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
}
