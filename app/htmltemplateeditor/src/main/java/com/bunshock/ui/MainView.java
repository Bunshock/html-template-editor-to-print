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
import com.bunshock.service.ProfileService;
import com.bunshock.service.ReportGenerator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainView {

    private final Stage stage;
    private final BorderPane rootLayout = new BorderPane();
    
    // UI Controls
    private final ComboBox<AppProfile> profileSelector = new ComboBox<>();
    private final VBox formContainer = new VBox(15);
    private final ScrollPane scrollPane = new ScrollPane(formContainer);
    
    // Data Management
    private final Map<AppProfile, File> profileSourceMap = new HashMap<>(); // Maps Profile -> File location
    
    // Current Form Data
    private Map<String, Control> currentInputMap = new HashMap<>();
    private Map<String, DynamicTableBuilder> currentTableMap = new HashMap<>();
    private final WebView invisibleBrowser = new WebView();

    public MainView(Stage stage) {
        this.stage = stage;
        setupUI();
    }

    private void setupUI() {
        // --- 1. Top Header (Selector) ---
        Label lblNota = new Label("Nota:");
        lblNota.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        profileSelector.setPromptText("Select a Profile...");
        profileSelector.setPrefWidth(200);
        profileSelector.setOnAction(e -> renderForm(profileSelector.getValue()));

        Button btnAdd = new Button("+");
        btnAdd.setTooltip(new Tooltip("Add Profile"));
        btnAdd.setOnAction(e -> addProfile());

        Button btnRemove = new Button("-");
        btnRemove.setTooltip(new Tooltip("Remove Current Profile"));
        btnRemove.setOnAction(e -> removeCurrentProfile());

        HBox header = new HBox(10, lblNota, profileSelector, btnAdd, btnRemove);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");

        // --- 2. Center (Form Area) ---
        formContainer.setPadding(new Insets(20));
        scrollPane.setFitToWidth(true);

        rootLayout.setTop(header);
        rootLayout.setCenter(scrollPane);

        // Initial State
        refreshState();
    }

    public void show() {
        stage.setScene(new Scene(rootLayout, 600, 800));
        stage.setTitle("Report Generator");
        stage.show();
    }

    // --- Profile Management ---

    public void loadProfileFromFile(File file) {
        try {
            AppProfile profile = ProfileService.loadProfile(file);
            profileSourceMap.put(profile, file);
            profileSelector.getItems().add(profile);
            profileSelector.getSelectionModel().select(profile); // Auto-select new profile
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error loading profile: " + e.getMessage()).show();
        }
    }

    private void addProfile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            loadProfileFromFile(file);
        }
    }

    private void removeCurrentProfile() {
        AppProfile selected = profileSelector.getValue();
        if (selected != null) {
            profileSelector.getItems().remove(selected);
            profileSourceMap.remove(selected);
            refreshState();
        }
    }

    // --- Rendering Logic ---

    private void refreshState() {
        if (profileSelector.getItems().isEmpty()) {
            showEmptyState();
        } else if (profileSelector.getValue() == null) {
            // List not empty, but nothing selected
            profileSelector.getSelectionModel().selectFirst();
        }
    }

    private void showEmptyState() {
        formContainer.getChildren().clear();
        
        Label lblEmpty = new Label("No profiles found.");
        lblEmpty.setStyle("-fx-font-size: 18px; -fx-text-fill: #666;");
        
        Button btnAdd = new Button("Add Profile");
        btnAdd.setStyle("-fx-font-size: 14px;");
        btnAdd.setOnAction(e -> addProfile());

        VBox emptyBox = new VBox(15, lblEmpty, btnAdd);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(50));
        
        rootLayout.setCenter(emptyBox); // Replace scrollpane with empty box
    }

    private void renderForm(AppProfile profile) {
        if (profile == null) return;

        // Restore ScrollPane if we were in empty state
        rootLayout.setCenter(scrollPane);
        
        // Clear previous form data
        formContainer.getChildren().clear();
        currentInputMap.clear();
        currentTableMap.clear();

        // 1. Simple Fields
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        int row = 0;

        for (FieldConfig field : profile.getSimpleFields()) {
            grid.add(new Label(field.getLabel() + ":"), 0, row);
            Control input;
            if ("DATE".equals(field.getType())) {
                input = new DatePicker(LocalDate.now());
            } else {
                input = new TextField();
            }
            currentInputMap.put(field.getTag(), input);
            grid.add(input, 1, row);
            row++;
        }
        formContainer.getChildren().add(grid);

        // 2. Tables
        for (TableConfig tableCfg : profile.getTables()) {
            DynamicTableBuilder builder = new DynamicTableBuilder();
            formContainer.getChildren().add(builder.createTable(tableCfg));
            currentTableMap.put(tableCfg.getTableName(), builder);
        }

        // 3. Print Button
        Button printBtn = new Button("Generate & Print");
        printBtn.setStyle("-fx-font-size: 14px; -fx-base: #4CAF50;");
        printBtn.setMaxWidth(Double.MAX_VALUE);
        printBtn.setOnAction(e -> handlePrint(profile));
        
        formContainer.getChildren().add(new Separator());
        formContainer.getChildren().add(printBtn);
    }

    // --- Printing Logic ---

    private void handlePrint(AppProfile profile) {
        try {
            File jsonFile = profileSourceMap.get(profile);
            File templateFile = PathHelper.resolveFullPath(jsonFile.getParentFile(), profile.getTemplatePath());

            if (templateFile == null || !templateFile.exists()) {
                new Alert(Alert.AlertType.ERROR, "Template missing: " + profile.getTemplatePath()).show();
                return;
            }

            String html = Files.readString(templateFile.toPath());
            ReportGenerator generator = new ReportGenerator();
            Map<String, String> simpleData = new HashMap<>();
            
            // AUTOMATIC TAG: Inject Profile Name
            simpleData.put("TEMPLATE_NAME", profile.getProfileName());

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (var entry : currentInputMap.entrySet()) {
                if (entry.getValue() instanceof DatePicker dp) {
                    simpleData.put(entry.getKey(), dp.getValue() != null ? dp.getValue().format(fmt) : "");
                } else if (entry.getValue() instanceof TextField tf) {
                    simpleData.put(entry.getKey(), tf.getText());
                }
            }

            html = generator.processSimpleFields(html, simpleData);
            
            for (var entry : currentTableMap.entrySet()) {
                html = generator.processTables(html, entry.getKey(), entry.getValue().getData());
            }

            WebEngine engine = invisibleBrowser.getEngine();
            engine.loadContent(html);
            engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                    printWeb(invisibleBrowser);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error generating report: " + ex.getMessage()).show();
        }
    }

    private void printWeb(WebView web) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(stage)) {
            web.getEngine().print(job);
            job.endJob();
        }
    }
}
