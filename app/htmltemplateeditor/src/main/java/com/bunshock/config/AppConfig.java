package com.bunshock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

public class AppConfig {
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final Properties properties = new Properties();
    private static final Preferences userPrefs = Preferences.userNodeForPackage(AppConfig.class);
    private static final String KEY_SERVER_PATH = "custom_server_path";

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        // Try external first (next to .exe)
        File externalFile = new File(CONFIG_FILE_NAME);
        if (externalFile.exists()) {
            try (InputStream input = new FileInputStream(externalFile)) {
                properties.load(input);
                return;
            } catch (IOException e) { System.err.println("Error reading external config."); }
        }

        // Fallback to internal (inside JAR)
        try (InputStream input = AppConfig.class.getResourceAsStream("/" + CONFIG_FILE_NAME)) {
            if (input != null) properties.load(input);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- NETWORK PATH LOGIC ---
    public static String getBasePath() {
        String savedPath = userPrefs.get(KEY_SERVER_PATH, null);
        if (savedPath != null && !savedPath.isEmpty()) return savedPath;
        return properties.getProperty("server.base.path", "//SERVIDOR/Carpeta");
    }

    public static void setCustomBasePath(String path) {
        if (path == null || path.isEmpty()) userPrefs.remove(KEY_SERVER_PATH);
        else userPrefs.put(KEY_SERVER_PATH, path);
    }

    public static String getOptionsFileName() {
        return properties.getProperty("options.filename", "options.json");
    }

    public static String getAppVersion() {
        return properties.getProperty("app.version", "1.0.0");
    }

    public static String getAppEmail() {
        return properties.getProperty("app.email", "example@example.com");
    }

    public static String getHowToText() {
        String text = properties.getProperty("app.howTo", "Instrucciones no disponibles.");
        return text.replace("\\n", "\n");
    }

    public static String getAppAuthor() {
        return properties.getProperty("app.author", "Autor Desconocido");
    }
}