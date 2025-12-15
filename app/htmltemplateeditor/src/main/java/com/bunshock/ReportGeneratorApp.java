package com.bunshock;

import java.io.File;
import java.util.prefs.Preferences;

import com.bunshock.ui.MainView;

import javafx.application.Application;
import javafx.stage.Stage;

public class ReportGeneratorApp extends Application {

    private static final String KEY_LAST_PROFILE = "last_profile";
    private Preferences prefs = Preferences.userNodeForPackage(ReportGeneratorApp.class);

    @Override
    public void start(Stage stage) {
        // 1. Create the Main View
        MainView view = new MainView(stage);

        // 2. Try to load the last used profile automatically
        String lastPath = prefs.get(KEY_LAST_PROFILE, null);
        if (lastPath != null) {
            File file = new File(lastPath);
            if (file.exists()) {
                view.loadProfileFromFile(file);
            }
        }

        // 3. Show the UI (It will handle empty states automatically)
        view.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}