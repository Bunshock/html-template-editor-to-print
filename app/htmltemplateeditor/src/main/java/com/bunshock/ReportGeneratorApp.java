package com.bunshock;

import com.bunshock.service.OptionsManager;
import com.bunshock.ui.MainView;

import javafx.application.Application;
import javafx.stage.Stage;

public class ReportGeneratorApp extends Application {

    @Override
    public void start(Stage stage) {
        // Load options early to prevent race conditions
        OptionsManager.getInstance();
        // Create the view. It will handle loading saved profiles itself.
        new MainView(stage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}