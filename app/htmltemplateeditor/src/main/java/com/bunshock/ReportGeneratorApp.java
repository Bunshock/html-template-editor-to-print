package com.bunshock;

import java.io.File;
import java.util.prefs.Preferences;

import com.bunshock.model.AppProfile;
import com.bunshock.service.ProfileService;
import com.bunshock.ui.DynamicFormView;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ReportGeneratorApp extends Application {

    private static final String KEY_LAST_PROFILE = "last_profile";
    private Preferences prefs = Preferences.userNodeForPackage(ReportGeneratorApp.class);

    @Override
    public void start(Stage stage) {
        String lastPath = prefs.get(KEY_LAST_PROFILE, null);
        File profileFile = (lastPath != null) ? new File(lastPath) : null;

        if (profileFile != null && profileFile.exists()) {
            loadAndLaunch(stage, profileFile);
        } else {
            chooseProfile(stage);
        }
    }

    private void chooseProfile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = fc.showOpenDialog(stage);

        if (file != null) {
            prefs.put(KEY_LAST_PROFILE, file.getAbsolutePath());
            loadAndLaunch(stage, file);
        }
    }

    private void loadAndLaunch(Stage stage, File file) {
        try {
            AppProfile profile = ProfileService.loadProfile(file);
            new DynamicFormView(profile, file).show(stage);
        } catch (Exception e) {
            e.printStackTrace();
            chooseProfile(stage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}