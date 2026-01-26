package com.bunshock.ui;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bunshock.model.FieldConfig;
import com.bunshock.service.OptionsManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;

public class DynamicComboCell extends TableCell<Map<String, String>, String> {

    private ComboBox<String> comboBox;
    private final String currentField; // e.g., "brand"

    private final FieldConfig config;

    public DynamicComboCell(FieldConfig config) {
        this.config = config;
        this.currentField = config.getTag();
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            Map<String, String> row = getTableView().getItems().get(getIndex());
            if (!CellUtils.isFieldEnabled(row, config)) {
                return; // STOP! Do not open the dropdown
            }
            super.startEdit();
            createComboBox();
            setText(null);
            setGraphic(comboBox);
            comboBox.show(); // Auto-open the dropdown
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        boolean enabled = true;
        if (!empty && getTableView() != null && getIndex() < getTableView().getItems().size()) {
            Map<String, String> row = getTableView().getItems().get(getIndex());
            enabled = CellUtils.isFieldEnabled(row, config);
        }

        // Visual feedback
        setDisable(!enabled); 
        setStyle(enabled ? "" : "-fx-background-color: #e0e0e0; -fx-text-fill: #888;"); // Gray out if disabled

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (comboBox != null) comboBox.setValue(item);
                setText(null);
                setGraphic(comboBox);
            } else {
                // If disabled, maybe show "N/A" or just the empty string?
                setText(enabled ? item : "N/A"); 
                setGraphic(null);
            }
        }
    }

    private void createComboBox() {
        // 1. Determine options based on Parent Value
        OptionsManager mgr = OptionsManager.getInstance();
        String parentField = mgr.getDependencyField(currentField);
        
        // We use a temporary variable for calculation
        String calculatedParentValue = null;

        // If there is a dependency, fetch the value from the current row
        if (parentField != null && getTableView() != null) {
            Map<String, String> rowData = getTableView().getItems().get(getIndex());
            calculatedParentValue = rowData.get(parentField);
        }
        
        // 'final' copy to use inside the Lambda below
        final String parentValue = calculatedParentValue;

        // 2. Fetch Options using the calculated value
        List<String> options = mgr.getOptions(currentField, parentValue);
        options.add(0, ""); // Allow empty selection
        options.add("Agregar..."); 

        comboBox = new ComboBox<>(FXCollections.observableArrayList(options));
        comboBox.setEditable(true);
        comboBox.setValue(getItem());

        // 3. Handle Selection
        comboBox.setOnAction(e -> {
            String selected = comboBox.getValue();
            if ("Agregar...".equals(selected)) {
                // Now passing 'parentValue' is legal because it is effectively final
                handleNewOption(parentValue);
            } else {
                commitEdit(selected);
            }
        });

        // 4. Handle Keys (Enter/Esc)
        comboBox.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.ESCAPE) cancelEdit();
        });
        
        // 5. Handle Focus Loss (Blur)
        comboBox.focusedProperty().addListener((obs, old, isFocused) -> {
            if (!isFocused && isEditing()) {
                String raw = comboBox.getEditor().getText();
                if(raw != null && !raw.equals("Agregar...") && !raw.isEmpty()) {
                     commitEdit(raw);
                } else {
                     cancelEdit();
                }
            }
        });
    }

    private void handleNewOption(String parentValue) {
        // Hide the combo so the dialog looks clean
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Agregar nueva opción");
            dialog.setHeaderText("Agregar nuevo valor para: " + config.getLabel());
            dialog.setContentText("Valor:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String newValue = result.get().trim();
                
                // 1. Save to JSON
                OptionsManager.getInstance().addOption(currentField, newValue, parentValue);
                
                // 2. Commit to Table
                commitEdit(newValue);
            } else {
                cancelEdit(); // User cancelled dialog
            }
        });
    }
}