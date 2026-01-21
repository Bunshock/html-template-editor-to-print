package com.bunshock.model;

import java.util.List;

public class FieldConfig {
    private String tag;     // e.g., {{NAME}}
    private String label;   // e.g., "Name"
    private String type;    // e.g., "TEXT", "DATE"
    private ConditionConfig enabledIf;

    public FieldConfig(String tag, String label, String type) {
        this.tag = tag;
        this.label = label;
        this.type = type;
    }

    // Getters

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ConditionConfig getEnabledIf() {
        return enabledIf;
    }

    public void setEnabledIf(ConditionConfig enabledIf) {
        this.enabledIf = enabledIf;
    }

    // Inner class for the JSON structure
    public static class ConditionConfig {
        public String tag;
        public List<String> values;
    }
}
