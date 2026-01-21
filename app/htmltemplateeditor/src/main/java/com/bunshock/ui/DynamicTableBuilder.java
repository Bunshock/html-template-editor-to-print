package com.bunshock.ui;

import java.util.HashMap;
import java.util.Map;

import com.bunshock.model.FieldConfig;
import com.bunshock.model.TableConfig;
import com.bunshock.service.OptionsManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class DynamicTableBuilder {
    // We keep a reference to the table to control focus later
    private TableView<Map<String, String>> table;
    private ObservableList<Map<String, String>> tableData = FXCollections.observableArrayList();
    
    // We need to keep track of the first editable column to trigger auto-edit
    private TableColumn<Map<String, String>, ?> firstEditableColumn = null;

    public VBox createTable(TableConfig config) {
        table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(200);

        // --- 1. VISUAL IMPROVEMENT: Row Number Column ---
        // This helps the user see exactly how many rows exist (even empty ones)
        TableColumn<Map<String, String>, Void> indexCol = new TableColumn<>("#");
        indexCol.setPrefWidth(40);
        indexCol.setMaxWidth(40);
        indexCol.setSortable(false);
        indexCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        table.getColumns().add(indexCol);

        // --- 2. Build Dynamic Columns ---
        for (FieldConfig colCfg : config.getColumns()) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(colCfg.getLabel());
            
            col.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault(colCfg.getTag(), ""))
            );
            
            // Check if this field exists in our OptionsManager
            if (OptionsManager.getInstance().hasOptions(colCfg.getTag())) {
                // Use the Smart Dropdown
                col.setCellFactory(column -> new DynamicComboCell(colCfg));
            } else {
                // Use the Standard Text Input (Save on Blur)
                col.setCellFactory(column -> new CommitOnBlurCell(colCfg));
            }
            
            col.setOnEditCommit(e -> {
                e.getRowValue().put(colCfg.getTag(), e.getNewValue());
                table.refresh();
            });

            table.getColumns().add(col);

            // Capture the first column so we can auto-edit it later
            if (firstEditableColumn == null) {
                firstEditableColumn = col;
            }
        }
        table.setItems(tableData);

        // --- 3. Buttons & Logic ---
        Button addBtn = new Button("Agregar item");
        addBtn.setOnAction(e -> handleAddItem(config));
        
        Button removeBtn = new Button("Eliminar selección");
        removeBtn.setOnAction(e -> {
            if(!table.getSelectionModel().isEmpty()) {
                tableData.remove(table.getSelectionModel().getSelectedItem());
            }
        });

        // --- 4. Item Counter ---
        Label counterLabel = new Label();
        counterLabel.textProperty().bind(Bindings.size(tableData).asString("Cantidad de items: %d"));

        // Layout: Buttons on left, spacer in middle, counter on right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox controls = new HBox(10, addBtn, removeBtn, spacer, counterLabel);
        
        return new VBox(5, table, controls);
    }

    // --- Logic to Handle "Add Item" Rules ---
    private void handleAddItem(TableConfig config) {
        // Rule 1: Don't add if previous row is empty
        if (!tableData.isEmpty()) {
            Map<String, String> lastRow = tableData.get(tableData.size() - 1);
            if (isRowEmpty(lastRow)) {
                showAlert("Fila incompleta", "Por favor complete la fila vacía antes de agregar una nueva.");
                
                // UX: Select the empty row so they see it
                table.getSelectionModel().select(tableData.size() - 1);
                table.scrollTo(tableData.size() - 1);
                if (firstEditableColumn != null) {
                    table.edit(tableData.size() - 1, firstEditableColumn);
                }
                return;
            }
        }

        // Create the new empty row
        Map<String, String> newRow = new HashMap<>();
        for (FieldConfig fc : config.getColumns()) {
            newRow.put(fc.getTag(), "");
        }
        tableData.add(newRow);

        // Rule 2: Focus and Edit the new row immediately
        int newIndex = tableData.size() - 1;
        table.getSelectionModel().select(newIndex);
        table.scrollTo(newIndex);
        table.requestFocus();
        
        if (firstEditableColumn != null) {
            table.edit(newIndex, firstEditableColumn);
        }
    }

    // Helper: Checks if all values in the row map are empty
    private boolean isRowEmpty(Map<String, String> row) {
        for (String value : row.values()) {
            if (value != null && !value.trim().isEmpty()) {
                return false; // Found a value, so it's not empty
            }
        }
        return true; // All values were empty or null
    }

    // Helper: Call this before printing to block empty rows
    public boolean hasEmptyRows() {
        for (Map<String, String> row : tableData) {
            if (isRowEmpty(row)) return true;
        }
        return false;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public ObservableList<Map<String, String>> getData() {
        return tableData;
    }
}