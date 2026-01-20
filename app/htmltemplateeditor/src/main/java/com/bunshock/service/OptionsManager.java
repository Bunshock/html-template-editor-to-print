package com.bunshock.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OptionsManager {
    private static OptionsManager instance;
    private final File file = new File("options.json");
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode rootNode;

    private OptionsManager() {
        load();
    }

    public static OptionsManager getInstance() {
        if (instance == null) instance = new OptionsManager();
        return instance;
    }

    private void load() {
        try {
            if (!file.exists()) {
                // Create default structure if missing
                rootNode = mapper.createObjectNode();
                save();
            } else {
                rootNode = mapper.readTree(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            rootNode = mapper.createObjectNode();
        }
    }

    private void save() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, rootNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets options for a field, filtering by the parent value if necessary.
     */
    public List<String> getOptions(String fieldName, String parentValue) {
        JsonNode fieldNode = rootNode.path(fieldName);
        if (fieldNode.isMissingNode()) return new ArrayList<>();

        JsonNode options = fieldNode.path("options");
        List<String> results = new ArrayList<>();

        if (options.isArray()) {
            for (JsonNode opt : options) {
                // Case 1: Simple String (No dependency)
                if (opt.isTextual()) {
                    results.add(opt.asText());
                } 
                // Case 2: Object with dependency logic
                else if (opt.isObject()) {
                    String val = opt.path("value").asText();
                    
                    // If no parent filter is provided (parentValue is null/empty), return EVERYTHING
                    if (parentValue == null || parentValue.isEmpty()) {
                        results.add(val);
                    } else {
                        // Check if this option belongs to the selected parent
                        JsonNode parents = opt.path("parents");
                        boolean match = StreamSupport.stream(parents.spliterator(), false)
                                .anyMatch(p -> p.asText().equalsIgnoreCase(parentValue));
                        if (match) {
                            results.add(val);
                        }
                    }
                }
            }
        }
        Collections.sort(results);
        return results;
    }

    /**
     * Adds a new option to the JSON and saves it.
     */
    public void addOption(String fieldName, String newValue, String parentValue) {
        ObjectNode fieldNode = (ObjectNode) rootNode.path(fieldName);
        if (fieldNode.isMissingNode()) {
            // Create field if it doesn't exist
            fieldNode = ((ObjectNode) rootNode).putObject(fieldName);
            fieldNode.putArray("options");
        }

        ArrayNode optionsArray = (ArrayNode) fieldNode.path("options");
        String dependency = fieldNode.path("dependsOn").asText(null);

        // Check if exists to avoid duplicates
        boolean exists = StreamSupport.stream(optionsArray.spliterator(), false)
            .anyMatch(node -> {
                if (node.isTextual()) return node.asText().equalsIgnoreCase(newValue);
                return node.path("value").asText().equalsIgnoreCase(newValue);
            });

        if (exists) return; // Already there

        if (dependency == null) {
            // Simple Add
            optionsArray.add(newValue);
        } else {
            // Complex Add (with Parent)
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
        return rootNode.has(fieldName);
    }
}