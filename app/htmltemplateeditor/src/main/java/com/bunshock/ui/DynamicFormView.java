package com.bunshock.ui;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.bunshock.model.AppProfile;
import com.bunshock.model.FieldConfig;
import com.bunshock.model.TableConfig;
import com.bunshock.service.PathHelper;
import com.bunshock.service.ReportGenerator;

import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DynamicFormView {
    private final AppProfile profile;
    private final File profileFile;
    private final Map<String, Control> inputMap = new HashMap<>();
    private final Map<String, DynamicTableBuilder> tableMap = new HashMap<>();
    private final WebView invisibleBrowser = new WebView();
    
    public DynamicFormView(AppProfile profile, File profileFile) {
        this.profile = profile;
        this.profileFile = profileFile;
    }

    public void show(Stage stage) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Simple fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int row = 0;

        for (FieldConfig field : profile.getSimpleFields()) {
            grid.add(new Label(field.getLabel() + ":"), 0, row);
            Control input;
            if ("DATE".equals(field.getType())) {
                DatePicker dp = new DatePicker(LocalDate.now());
                input = dp;
            } else {
                input = new TextField();
            }
            inputMap.put(field.getTag(), input);
            grid.add(input, 1, row);
            row++;
        }
        content.getChildren().add(grid);

        // Tables
        for (TableConfig tableCfg : profile.getTables()) {
            DynamicTableBuilder builder = new DynamicTableBuilder();
            content.getChildren().add(builder.createTable(tableCfg));
            tableMap.put(tableCfg.getTableName(), builder);
        }

        // Print button
        Button printBtn = new Button("Generate & Print");
        printBtn.setStyle("-fx-font-size: 14px; -fx-base: #4CAF50;");
        printBtn.setOnAction(e -> handlePrint());
        content.getChildren().add(new Separator());
        content.getChildren().add(printBtn);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        stage.setScene(new Scene(scroll, 500, 700));
        stage.setTitle("Report: " + profile.getProfileName());
        stage.show();
    }

    private void handlePrint() {
        try {
            // Load template
            File templateFile = PathHelper.resolveFullPath(profileFile.getParentFile(), profile.getTemplatePath());
            if (templateFile == null || !templateFile.exists()) {
                new Alert(Alert.AlertType.ERROR, "Template missing: " + profile.getTemplatePath()).show();
                return;
            }
            String html  = Files.readString(templateFile.toPath());
            
            // Prepare data
            ReportGenerator generator = new ReportGenerator();
            Map<String, String> simpleData = new HashMap<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (var entry : inputMap.entrySet()) {
                if (entry.getValue() instanceof DatePicker dp) {
                    simpleData.put(entry.getKey(), dp.getValue().format(fmt));
                } else if (entry.getValue() instanceof TextField tf) {
                    simpleData.put(entry.getKey(), tf.getText());
                }
            }

            // Process HTML
            html = generator.processSimpleFields(html, simpleData);

            for (var entry : tableMap.entrySet()) {
                html = generator.processTables(html, entry.getKey(), entry.getValue().getData());
            }

            // Load and print
            WebEngine engine = invisibleBrowser.getEngine();
            engine.loadContent(html);
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    printWeb(invisibleBrowser);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printWeb(WebView web) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            web.getEngine().print(job);
            job.endJob();
        }
    }
}
