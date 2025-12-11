package com.bunshock.model;

public class FieldConfig {
    private String tag;     // e.g., {{NAME}}
    private String label;   // e.g., "Name"
    private String type;    // e.g., "TEXT", "DATE"

    public FieldConfig(String tag, String label, String type) {
        this.tag = tag;
        this.label = label;
        this.type = type;
    }

    // Getters

    public String getTag() {
        return tag;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }
}
