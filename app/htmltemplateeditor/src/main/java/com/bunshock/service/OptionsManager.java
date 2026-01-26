package com.bunshock.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
        loadOptionsFromBackup();
        loadOptionsFromServer();
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
        // --- STAGE 1: IMMEDIATE LOCAL LOAD (Prevents 'null' state) ---
        try {
            if (localBackupFile.exists()) {
                JsonNode backupNode = mapper.readTree(localBackupFile);
                // Only use the backup if it's not a "null" literal or empty
                if (backupNode != null && !backupNode.isNull()) {
                    this.rootNode = backupNode;
                    
                    // Initialize the timestamp from the file's last modified date
                    long lastModified = localBackupFile.lastModified();
                    String savedTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                    
                    Platform.runLater(() -> {
                        lastSyncTime.set(savedTime);
                        connectionStatus.set("Offline (Copia Local)");
                    });
                    System.out.println("Cargada copia local de: " + savedTime);
                }
            }
            
            // Safety: If after trying to load backup rootNode is still null, create an empty object
            if (this.rootNode == null) {
                this.rootNode = mapper.createObjectNode();
            }
        } catch (IOException e) {
            this.rootNode = mapper.createObjectNode();
            System.err.println("Error leyendo backup local: " + e.getMessage());
        }

        // --- STAGE 2: ASYNC SERVER SYNC ---
        new Thread(() -> {
            try {
                Path serverPath = Paths.get(AppConfig.getBasePath(), AppConfig.getOptionsFileName());
                
                if (Files.exists(serverPath)) {
                    // 1. Fetch from server into a temp variable
                    JsonNode newNode = mapper.readTree(serverPath.toFile());
                    
                    // 2. VALIDATION: Ensure the server data is valid before swapping
                    if (newNode != null && !newNode.isNull() && newNode.isObject()) {
                        this.rootNode = newNode;
                        
                        // 3. Overwrite the backup only with VALID data
                        mapper.writerWithDefaultPrettyPrinter().writeValue(localBackupFile, this.rootNode);
                        
                        // 4. Update UI
                        updateSuccessfulSyncTime();
                        Platform.runLater(() -> connectionStatus.set("Online (Servidor)"));
                        System.out.println("Sincronización exitosa con: " + serverPath);
                    }
                } else {
                    throw new NoSuchFileException("No se encuentra el archivo en el servidor");
                }
            } catch (Exception e) {
                // Server failed? No problem, we already have the local backup loaded from Stage 1.
                Platform.runLater(() -> connectionStatus.set("Offline (Error de conexión)"));
                System.err.println("Sincronización fallida (usando backup): " + e.getMessage());
            }
        }).start();
    }

    private void loadOptionsFromBackup() {
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

    public void loadOptionsFromServer() {
        new Thread(() -> {
            try {
                Path serverPath = Paths.get(AppConfig.getBasePath(), AppConfig.getOptionsFileName());
                
                if (Files.exists(serverPath)) {
                    // Success! Read the server file
                    JsonNode newNode = mapper.readTree(serverPath.toFile());
                    
                    if (newNode != null && newNode.isNull()) {
                        // Update the logic in memory
                        rootNode = newNode;
                        
                        // Update the local backup file so it's fresh for the next session
                        mapper.writerWithDefaultPrettyPrinter().writeValue(localBackupFile, rootNode);
                        
                        // Update UI: status and NEW successful sync time
                        updateSuccessfulSyncTime();
                        Platform.runLater(() -> connectionStatus.set("Online (Servidor)"));
                        
                        System.out.println("Sincronización con servidor completada.");
                    }
                } else {
                    // Server file not found, but we already have the backup loaded, so we just update status
                    Platform.runLater(() -> connectionStatus.set("Offline (Servidor no alcanzado)"));
                }
            } catch (Exception e) {
                // Network error or timeout: UI already has the backup data, just notify user
                Platform.runLater(() -> connectionStatus.set("Offline (Error de red)"));
                System.err.println("Error durante la sincronización: " + e.getMessage());

                // Reload from backup to ensure consistency
                loadOptionsFromBackup();
            }
        }).start();
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
        // If rootNode is null or just an empty marker, DON'T overwrite the file
        if (rootNode == null || rootNode.isMissingNode() || (rootNode.isObject() && rootNode.isEmpty())) {
            System.out.println("Guardado abortado: rootNode está vacío o es nulo.");
            return;
        }

        try {
            // Save to local backup
            mapper.writerWithDefaultPrettyPrinter().writeValue(localBackupFile, rootNode);
            
            // Try to save to server if you have write permissions
            Path serverPath = Paths.get(AppConfig.getBasePath(), AppConfig.getOptionsFileName());
            if (Files.isWritable(serverPath.getParent())) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(serverPath.toFile(), rootNode);
            }
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