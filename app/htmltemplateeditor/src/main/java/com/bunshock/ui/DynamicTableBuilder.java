package com.bunshock.ui;

import java.util.HashMap;
import java.util.Map;

import com.bunshock.model.FieldConfig;
import com.bunshock.model.TableConfig;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DynamicTableBuilder {
    private ObservableList<Map<String, String>> tableData = FXCollections.observableArrayList();

    public VBox createTable(TableConfig config) {
        TableView<Map<String, String>> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(200);

        for (FieldConfig colCfg : config.getColumns()) {
            TableColumn<Map<String,String>, String> col = new TableColumn<>(colCfg.getLabel());
            
            col.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault(colCfg.getTag(), ""))
            );
            col.setCellFactory(TextFieldTableCell.forTableColumn());
            col.setOnEditCommit(e -> e.getRowValue().put(colCfg.getTag(), e.getNewValue()));

            table.getColumns().add(col);
        }
        table.setItems(tableData);

        Button addBtn = new Button("Add Item");
        addBtn.setOnAction(e -> {
            Map<String, String> newRow = new HashMap<>();
            for (FieldConfig fc : config.getColumns()) {
                newRow.put(fc.getTag(), "");
            }
            tableData.add(newRow);
        });
        
        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            if(!table.getSelectionModel().isEmpty()) {
                tableData.remove(table.getSelectionModel().getSelectedItem());
            }
        });

        HBox buttons = new HBox(10, addBtn, removeBtn);
        return new VBox(5, new Label("List: " + config.getTableName()), table, buttons);
    }

    public ObservableList<Map<String, String>> getData() {
        return tableData;
    }
}
