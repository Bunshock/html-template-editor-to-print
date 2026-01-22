package com.bunshock.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import com.bunshock.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OptionsManager {
    private static OptionsManager instance;
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode rootNode;
    private final StringProperty lastSyncTime = new SimpleStringProperty("Última sincronización: N/A");
    
    // BACKUP FILE: Always saved locally in the app folder
    private final File localBackupFile = new File("local_backup_options.json");
    
    // UI STATUS: Bind this to your label in MainView
    private final StringProperty connectionStatus = new SimpleStringProperty("Intentando sincronización...");

    private OptionsManager() {
        loadOptionsWithFallback();
    }

    public static synchronized OptionsManager getInstance() {
        if (instance == null) instance = new OptionsManager();
        return instance;
    }

    private void updateSuccessfulSyncTime() {
        Platform.runLater(() -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
            lastSyncTime.set(time);
        });
    }

    /**
     * MAIN SYNC LOGIC:
     * Tries Server -> If fails, tries Local Backup -> If fails, creates empty.
     */
    public void loadOptionsWithFallback() {
        // 1. Initial Check: If we have a backup, set the timestamp to the file's last modified date
        if (localBackupFile.exists()) {
            long lastModified = localBackupFile.lastModified();
            String savedTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
            lastSyncTime.set(savedTime);
        }

        new Thread(() -> {
            try {
                Path serverPath = Paths.get(AppConfig.getBasePath(), AppConfig.getOptionsFileName());
                
                if (Files.exists(serverPath)) {
                    // ... (Load JSON as before) ...
                    
                    // 2. Overwrite backup (This updates the file's 'Last Modified' timestamp on Windows)
                    mapper.writerWithDefaultPrettyPrinter().writeValue(localBackupFile, rootNode);
                    
                    // 3. Update UI with the NEW time
                    updateSuccessfulSyncTime();
                    updateStatus("Online (Servidor)");
                } else {
                    throw new IOException();
                }
            } catch (Exception e) {
                loadFromBackup();
            }
        }).start();
    }

    private void loadFromBackup() {
        try {
            if (localBackupFile.exists()) {
                rootNode = mapper.readTree(localBackupFile);
                // We do NOT call updateSuccessfulSyncTime here.
                // The label will continue showing the time of the last TRUE server sync.
                updateStatus("Offline (Copia local)");
            } else {
                rootNode = mapper.createObjectNode();
                updateStatus("Offline (Sin datos)");
            }
        } catch (IOException e) {
            updateStatus("Error de lectura");
        }
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> {
            connectionStatus.set(status);
            // Update the timestamp whenever the status changes to a "Success" state
            if (status.contains("Online") || status.contains("Usando copia local")) {
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                lastSyncTime.set("Última sincronización: " + time);
            }
        });
    }

    public StringProperty connectionStatusProperty() {
        return connectionStatus;
    }

    private void save() {
        try {
            // Save to the LOCAL backup.
            mapper.writerWithDefaultPrettyPrinter().writeValue(localBackupFile, rootNode);
            
            // Try to save to server too if online
            Path serverPath = Paths.get(AppConfig.getBasePath(), AppConfig.getOptionsFileName());
            mapper.writerWithDefaultPrettyPrinter().writeValue(serverPath.toFile(), rootNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getOptions(String fieldName, String parentValue) {
        if (rootNode == null) return new ArrayList<>();
        JsonNode fieldNode = rootNode.path(fieldName);
        if (fieldNode.isMissingNode()) return new ArrayList<>();

        JsonNode options = fieldNode.path("options");
        List<String> results = new ArrayList<>();

        if (options.isArray()) {
            for (JsonNode opt : options) {
                if (opt.isTextual()) {
                    results.add(opt.asText());
                } else if (opt.isObject()) {
                    String val = opt.path("value").asText();
                    if (parentValue == null || parentValue.isEmpty()) {
                        results.add(val);
                    } else {
                        JsonNode parents = opt.path("parents");
                        boolean match = StreamSupport.stream(parents.spliterator(), false)
                                .anyMatch(p -> p.asText().equalsIgnoreCase(parentValue));
                        if (match) results.add(val);
                    }
                }
            }
        }
        Collections.sort(results);
        return results;
    }

    public void addOption(String fieldName, String newValue, String parentValue) {
        ObjectNode fieldNode = (ObjectNode) rootNode.path(fieldName);
        if (fieldNode.isMissingNode()) {
            fieldNode = ((ObjectNode) rootNode).putObject(fieldName);
            fieldNode.putArray("options");
        }

        ArrayNode optionsArray = (ArrayNode) fieldNode.path("options");
        String dependency = fieldNode.path("dependsOn").asText(null);

        boolean exists = StreamSupport.stream(optionsArray.spliterator(), false)
            .anyMatch(node -> {
                if (node.isTextual()) return node.asText().equalsIgnoreCase(newValue);
                return node.path("value").asText().equalsIgnoreCase(newValue);
            });

        if (exists) return;

        if (dependency == null) {
            optionsArray.add(newValue);
        } else {
            ObjectNode newOpt = optionsArray.addObject();
            newOpt.put("value", newValue);
            ArrayNode parents = newOpt.putArray("parents");
            if (parentValue != null && !parentValue.isEmpty()) {
                parents.add(parentValue);
            }
        }
        save();
    }

    public String getDependencyField(String fieldName) {
        return rootNode.path(fieldName).path("dependsOn").asText(null);
    }

    public boolean hasOptions(String fieldName) {
        // Check if rootNode exists and has the field as a key
        return rootNode != null && rootNode.has(fieldName);
    }

    public StringProperty lastSyncTimeProperty() {
        return lastSyncTime;
    }
}