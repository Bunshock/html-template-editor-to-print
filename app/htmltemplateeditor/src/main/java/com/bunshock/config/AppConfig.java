package com.bunshock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE_NAME = "config.properties";

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        // 1. Try to load external file (next to .exe)
        File externalFile = new File(CONFIG_FILE_NAME);
        if (externalFile.exists()) {
            try (InputStream input = new FileInputStream(externalFile)) {
                properties.load(input);
                return; 
            } catch (IOException e) {
                System.err.println("Could not read external config.");
            }
        }

        // 2. Fallback: Load internal (from src/main/resources)
        try (InputStream input = AppConfig.class.getResourceAsStream("/" + CONFIG_FILE_NAME)) {
            if (input != null) properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getBasePath() {
        return properties.getProperty("server.base.path", "//localhost/SharedStorage");
    }

    public static String getOptionsFileName() {
        return properties.getProperty("options.file.name", "options.json");
    }
    
    public static String getTemplatesFolderName() {
        return properties.getProperty("templates.folder.name", "templates/");
    }
}