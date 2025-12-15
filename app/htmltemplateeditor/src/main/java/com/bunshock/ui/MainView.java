package com.bunshock.ui;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

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
import javafx.scene.control.ButtonType;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainView {

    private final Stage stage;
    private final BorderPane rootLayout = new BorderPane();
    
    // Preferences for session memory
    private final Preferences prefs = Preferences.userNodeForPackage(MainView.class);
    private static final String KEY_SAVED_PROFILES = "saved_profile_paths";

    // UI Controls
    private final ComboBox<AppProfile> profileSelector = new ComboBox<>();
    private final VBox formContainer = new VBox(15);
    private final ScrollPane scrollPane = new ScrollPane(formContainer);
    
    // Data Management
    private final Map<AppProfile, File> profileSourceMap = new HashMap<>();
    
    // Current Form Data
    private Map<String, Control> currentInputMap = new HashMap<>();
    private Map<String, DynamicTableBuilder> currentTableMap = new HashMap<>();
    private final WebView invisibleBrowser = new WebView();

    public MainView(Stage stage) {
        this.stage = stage;
        setupUI();
    }

    private void setupUI() {
        // --- 1. Top Header ---
        Label lblNota = new Label("Nota:");
        lblNota.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        profileSelector.setPromptText("Select a Profile...");
        profileSelector.setPrefWidth(200);
        
        // Listener to handle selection changes (User or Code)
        profileSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                renderForm(newVal);
            } else {
                showEmptyState();
            }
        });

        Button btnAdd = new Button("+");
        btnAdd.setTooltip(new Tooltip("Add Profile"));
        btnAdd.setOnAction(e -> addProfile());

        Button btnRemove = new Button("-");
        btnRemove.setTooltip(new Tooltip("Remove Current Profile"));
        btnRemove.setOnAction(e -> removeCurrentProfile());

        // SEPARATOR
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Pushes Reset button to the right

        Button btnReset = new Button("Reset Settings");
        btnReset.setStyle("-fx-text-fill: red;"); // Warning color
        btnReset.setTooltip(new Tooltip("Clear all saved profiles"));
        btnReset.setOnAction(e -> handleReset());

        HBox header = new HBox(10, lblNota, profileSelector, btnAdd, btnRemove, spacer, btnReset);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");

        // --- 2. Center (Form Area) ---
        // CENTER FIX: Align VBox content to top-center
        formContainer.setAlignment(Pos.TOP_CENTER);
        formContainer.setPadding(new Insets(30));
        
        // Ensure scrollpane fits width but allows centering
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        rootLayout.setTop(header);
        rootLayout.setCenter(scrollPane);

        // Start empty by default
        showEmptyState();
    }

    public void show() {
        stage.setScene(new Scene(rootLayout, 700, 800)); // Slightly wider default
        stage.setTitle("Report Generator");
        stage.show();
        
        restoreSession(); 
    }

    // --- Session Persistence ---

    private void saveSession() {
        String allPaths = profileSourceMap.values().stream()
                .map(File::getAbsolutePath)
                .distinct()
                .collect(Collectors.joining(File.pathSeparator));
        
        prefs.put(KEY_SAVED_PROFILES, allPaths);
    }

    private void restoreSession() {
        String allPaths = prefs.get(KEY_SAVED_PROFILES, "");
        if (allPaths.isEmpty()) return;

        String[] paths = allPaths.split(File.pathSeparator);
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                loadProfileFromFile(f, false);
            }
        }
        
        if (!profileSelector.getItems().isEmpty()) {
            profileSelector.getSelectionModel().selectFirst();
        }
    }
    
    private void handleReset() {
        // 1. Create the Confirmation Dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Settings");
        alert.setHeaderText("Clear all saved profiles?");
        alert.setContentText("This will remove all profiles from your history. This action cannot be undone.");

        // 2. Show and Wait for User Response
        // The '.showAndWait()' method blocks execution until the user clicks a button
        var result = alert.showAndWait();

        // 3. Execute only if User clicked OK
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // --- Reset Logic ---
                prefs.remove(KEY_SAVED_PROFILES);
                profileSourceMap.clear();
                
                // Force UI Reset (Prompt Text fix)
                profileSelector.getSelectionModel().clearSelection();
                profileSelector.setValue(null);
                profileSelector.getItems().clear(); 

                // Optional: Show success message
                new Alert(Alert.AlertType.INFORMATION, "Settings have been cleared.").show();
                
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Could not clear settings: " + e.getMessage()).show();
            }
        }
    }

    // --- Profile Management ---

    private void loadProfileFromFile(File file, boolean saveState) {
        try {
            AppProfile profile = ProfileService.loadProfile(file);
            profileSourceMap.put(profile, file);
            profileSelector.getItems().add(profile);
            
            profileSelector.getSelectionModel().select(profile);
            
            if (saveState) saveSession();
            
        } catch (Exception e) {
            System.err.println("Error loading profile: " + file.getName());
        }
    }
    
    // Overloaded helper
    private void loadProfileFromFile(File file) { loadProfileFromFile(file, true); }

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
            profileSourceMap.remove(selected);
            profileSelector.getItems().remove(selected);
            saveSession();
            
            if (profileSelector.getItems().isEmpty()) {
                profileSelector.getSelectionModel().clearSelection();
            } else {
                profileSelector.getSelectionModel().selectFirst();
            }
        }
    }

    // --- Rendering Logic ---

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
        
        rootLayout.setCenter(emptyBox);
    }

    private void renderForm(AppProfile profile) {
        rootLayout.setCenter(scrollPane);
        
        formContainer.getChildren().clear();
        currentInputMap.clear();
        currentTableMap.clear();

        // 1. Simple Fields
        GridPane grid = new GridPane();
        grid.setHgap(15); 
        grid.setVgap(15);
        
        // CENTER FIX: Align the grid itself to the center
        grid.setAlignment(Pos.CENTER);
        
        // CENTER FIX: Limit width so it doesn't stretch too wide
        grid.setMaxWidth(600); 

        int row = 0;
        for (FieldConfig field : profile.getSimpleFields()) {
            Label lbl = new Label(field.getLabel() + ":");
            lbl.setStyle("-fx-font-weight: bold;");
            grid.add(lbl, 0, row);
            
            Control input;
            if ("DATE".equals(field.getType())) {
                DatePicker dp = new DatePicker(LocalDate.now());
                dp.setPrefWidth(300); // Consistent width
                input = dp;
            } else {
                TextField tf = new TextField();
                tf.setPrefWidth(300); // Consistent width
                input = tf;
            }
            currentInputMap.put(field.getTag(), input);
            grid.add(input, 1, row);
            row++;
        }
        formContainer.getChildren().add(grid);

        // 2. Tables
        for (TableConfig tableCfg : profile.getTables()) {
            DynamicTableBuilder builder = new DynamicTableBuilder();
            // CENTER FIX: Wrap table in a box with max width
            VBox tableBox = builder.createTable(tableCfg);
            tableBox.setMaxWidth(600);
            formContainer.getChildren().add(tableBox);
            currentTableMap.put(tableCfg.getTableName(), builder);
        }

        // 3. Print Button
        Button printBtn = new Button("Generate & Print");
        printBtn.setStyle("-fx-font-size: 14px; -fx-base: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        printBtn.setPrefWidth(200); // Fixed nice width
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
            
            // AUTOMATIC TAG
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