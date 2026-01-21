package com.bunshock.ui;

import java.util.Map;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

public class DeleteButtonCell extends TableCell<Map<String, String>, String> {
    private final Button deleteButton = new Button("✕");

    public DeleteButtonCell() {
        // Style the button to look like a small red 'X'
        deleteButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #cc0000; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand;"
        );
        
        // Action: Remove the item from the table's underlying list
        deleteButton.setOnAction(e -> {
            if (getTableView() != null && getIndex() >= 0) {
                getTableView().getItems().remove(getIndex());
            }
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(deleteButton);
        }
    }
}