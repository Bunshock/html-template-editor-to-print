package com.bunshock.ui;

import java.util.Map;

import com.bunshock.model.FieldConfig;

import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * A custom TableCell that commits the edit when the user clicks outside (blur)
 * or presses ENTER. It cancels only on ESCAPE.
 */
public class CommitOnBlurCell extends TableCell<Map<String, String>, String> {

    private TextField textField;

    private final FieldConfig config;

    public CommitOnBlurCell(FieldConfig config) {
        this.config = config;
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            Map<String, String> row = getTableView().getItems().get(getIndex());
            if (!CellUtils.isFieldEnabled(row, config)) {
                return; // Block editing
            }
            super.startEdit();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText((String) getItem());
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
        
        setDisable(!enabled);
        setStyle(enabled ? "" : "-fx-background-color: #e0e0e0;");

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getString());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getString());
                setGraphic(null);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        
        // Key Logic:
        // 1. Enter = Save, Esc = Cancel
        textField.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.ENTER) {
                commitEdit(textField.getText());
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

        // 2. Focus Lost (Blur) = Save
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                commitEdit(textField.getText());
            }
        });
    }

    private String getString() {
        return getItem() == null ? "" : getItem();
    }
}