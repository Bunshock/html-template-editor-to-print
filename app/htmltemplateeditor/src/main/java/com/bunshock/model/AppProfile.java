package com.bunshock.model;

import java.util.ArrayList;
import java.util.List;

public class AppProfile {
    private String profileName;
    private String templatePath;
    private List<FieldConfig> simpleFields = new ArrayList<>();
    private List<TableConfig> tables = new ArrayList<>();

    public AppProfile() {}

    // Getters and Setters

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public List<FieldConfig> getSimpleFields() {
        return simpleFields;
    }

    public void setSimpleFields(List<FieldConfig> simpleFields) {
        this.simpleFields = simpleFields;
    }

    public List<TableConfig> getTables() {
        return tables;
    }

    public void setTables(List<TableConfig> tables) {
        this.tables = tables;
    }

    @Override
    public String toString() {
        return profileName != null ? profileName : "<Unnamed Profile>";
    }
}
