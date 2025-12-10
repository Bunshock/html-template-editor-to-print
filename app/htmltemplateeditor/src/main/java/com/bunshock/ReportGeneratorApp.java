package com.bunshock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ReportGeneratorApp extends Application {

    // UI inputs
    private TextField nameField = new TextField();
    private DatePicker datePicker = new DatePicker(LocalDate.now());
    private TextField brandField = new TextField();
    private TextField modelField = new TextField();
    private TextField serialField = new TextField();
    
    // Invisible browser to handle the HTML rendering/printing
    private WebView invisibleBrowser = new WebView();

    @Override
    public void start(Stage primaryStage) {
        // 1. Build the input form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(20));

        formGrid.addRow(0, new Label("Date:"), datePicker);
        formGrid.addRow(1, new Label("Customer Name:"), nameField);
        formGrid.addRow(2, new Label("Equipment Brand:"), brandField);
        formGrid.addRow(3, new Label("Model:"), modelField);
        formGrid.addRow(4, new Label("Serial Number:"), serialField);

        Button printBtn = new Button("Generate & Print");
        printBtn.setStyle("-fx-font-size: 14px; -fx-base: #4CAF50;");
        
        // 2. Action logic
        printBtn.setOnAction(e -> handlePrint());

        VBox root = new VBox(20, formGrid, printBtn);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 400, 350);
        primaryStage.setTitle("Service Report Generator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handlePrint() {
        try {
            // A. Load the template, assuming template.html is in the project root
            String template = Files.readString(Path.of("template.html"));

            // B. Replace Placeholders with user input. Serial numbers are uppercased.
            String filledHtml = template
                    .replace("{{DATE}}", datePicker.getValue().toString())
                    .replace("{{NAME}}", nameField.getText())
                    .replace("{{BRAND}}", brandField.getText())
                    .replace("{{MODEL}}", modelField.getText())
                    .replace("{{SERIAL}}", serialField.getText().toUpperCase());

            // C. Load into the engine
            WebEngine engine = invisibleBrowser.getEngine();
            engine.loadContent(filledHtml);

            // D. Wait for it to load, then print
            // We need a listener to ensure content is fully rendered before printing
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    printHtml(invisibleBrowser);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error reading template: " + ex.getMessage());
            alert.show();
        }
    }

    private void printHtml(WebView webView) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            // Using the WebView's engine to print allows it to handle 
            // the HTML/CSS rendering automatically
            boolean success = job.printPage(webView);
            if (success) {
                job.endJob();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}