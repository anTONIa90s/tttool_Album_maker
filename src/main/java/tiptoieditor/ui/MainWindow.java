package tiptoieditor.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import service.audio.AudioConvertService;
import service.audio.AudioCopyService;
import service.audio.AudioFileNameService;
import service.tonie.TonieAudioExportService;
import service.tonie.TonieExportDestinationService;
import service.tttool.TttoolService;
import service.workflow.AlbumFolderWorkflowResolver;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.io.File;

public class MainWindow {

        private TextArea outputArea;
        private Label workflowStatusLabel;
        private ProgressIndicator workflowStatusSpinner;

        private TextField productNameField;
        private TextField productIdField;
        private ToggleSwitch appendProductIdToggle;
        private final ValidationSupport validationSupport = new ValidationSupport();
        private RowConvertAudio rowConvertAudio;
        private RowCreateYaml rowCreateYaml;
        private RowYamlToGme rowYamlToGme;
        private RowCreateOidTable rowCreateOidTable;
        private RowExportTonieAudio rowExportTonieAudio;
        private AlbumWorkflowContinuation albumWorkflowContinuation;

        private final WorkflowTaskManager taskManager = new WorkflowTaskManager();
        private final TttoolService tttoolService = new TttoolService(taskManager);
        private final AudioCopyService audioCopyService = new AudioCopyService();
        private final AudioConvertService audioConvertService = new AudioConvertService(this::log, taskManager);
        private final AudioFileNameService audioFileNameService = new AudioFileNameService();
        private final TonieAudioExportService tonieAudioExportService = new TonieAudioExportService();
        private final TonieExportDestinationService tonieExportDestinationService = new TonieExportDestinationService();
        private final AlbumFolderWorkflowResolver workflowResolver = new AlbumFolderWorkflowResolver(
                        tonieAudioExportService);

        public void show(Stage stage) {
                Label title = new Label("TTTool GUI");
                title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

                productNameField = new TextField();
                productNameField.setPromptText("Album name (default: tttoolAlbum)");

                productIdField = new TextField();
                productIdField.setTextFormatter(
                                new TextFormatter<>(
                                                change -> change.getControlNewText().matches("\\d*") ? change : null));
                productIdField.setPrefColumnCount(5);
                productIdField.setPromptText("Product ID");
                validationSupport.registerValidator(productIdField,
                                Validator.createPredicateValidator(MainWindow::isValidProductId,
                                                "Enter a Product ID between 0 and " + Integer.MAX_VALUE + "."));

                appendProductIdToggle = new ToggleSwitch("Append ID");
                appendProductIdToggle.setTooltip(new javafx.scene.control.Tooltip(
                                "Append the Product ID to the album name (for example, name_890)"));

                Button selectDirectoryButton = new Button("Select Directory");
                Label selectedFolderPathLabel = new Label("No folder selected");
                selectedFolderPathLabel.setStyle("-fx-text-fill: gray;");
                HBox inputControlsRow = new HBox(10, selectDirectoryButton, productNameField, productIdField,
                                appendProductIdToggle);
                inputControlsRow.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(productNameField, Priority.ALWAYS);
                VBox rowInput = new VBox(4, selectedFolderPathLabel, inputControlsRow);

                Button runButton = new Button("Run tttool");
                Button cancelButton = new Button("Cancel");
                cancelButton.setVisible(false);
                cancelButton.setManaged(false);
                HBox runRow = new HBox(10, runButton, cancelButton);
                runRow.setMaxWidth(Double.MAX_VALUE);
                runButton.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(runButton, Priority.ALWAYS);

                Button selectAudioFolderButton = new Button("Select Audio Folder");
                Label selectedAudioFolderLabel = new Label("No folder selected");
                Button convertAudioButton = new Button("Convert Audio");

                Button selectAlbumFolderButton = new Button("Select Album Folder");
                Label selectedAlbumFolderLabel = new Label("No folder selected");
                Button createYamlButton = new Button("Create YAML");

                Button selectYamlFileButton = new Button("Select YAML file");
                Label selectedYamlFileLabel = new Label("No YAML file selected");
                Button createGmeButton = new Button("Create GME");

                Button selectOidTableFolderButton = new Button("Select Album Folder");
                Label selectedOidTableFolderLabel = new Label("No folder selected");
                Button createOidTableButton = new Button("Create OID Table");

                Button selectTonieFileButton = new Button("Select Tonie File");
                Label selectedTonieFileLabel = new Label("No file selected");
                Button exportTonieAudioButton = new Button("Export OGG");

                Button selectGmeFolderButton = new Button("Select GME Folder");
                Label selectedGmeFolderLabel = new Label("No folder selected");
                Button listGmeProductIdsButton = new Button("List Product IDs");
                ProgressIndicator listGmeProductIdsSpinner = new ProgressIndicator();
                listGmeProductIdsSpinner.setPrefSize(16, 16);
                listGmeProductIdsSpinner.setMaxSize(16, 16);
                listGmeProductIdsSpinner.setVisible(false);
                ObservableList<RowListGmeProductIds.ProductIdTableRow> productIdRows = FXCollections
                                .observableArrayList();
                TableView<RowListGmeProductIds.ProductIdTableRow> productIdTable = createProductIdTable(productIdRows);

                rowConvertAudio = new RowConvertAudio(stage, selectAudioFolderButton, selectedAudioFolderLabel,
                                convertAudioButton, audioCopyService, audioConvertService,
                                this::getAlbumName, this::log, this::setWorkflowStatus, taskManager);
                selectDirectoryButton.setOnAction(e -> rowConvertAudio.selectAudioFolder(stage));
                rowYamlToGme = new RowYamlToGme(stage, selectYamlFileButton, selectedYamlFileLabel,
                                createGmeButton, tttoolService, audioFileNameService, this::log,
                                this::setWorkflowStatus, taskManager);
                rowCreateOidTable = new RowCreateOidTable(stage, selectOidTableFolderButton,
                                selectedOidTableFolderLabel, createOidTableButton, tttoolService, this::log,
                                this::setWorkflowStatus, taskManager);
                rowYamlToGme.setOnSelectedYamlFile(
                                yamlFile -> rowCreateOidTable.setSelectedAlbumFolder(yamlFile.getParentFile()));
                rowCreateYaml = new RowCreateYaml(stage, selectAlbumFolderButton, selectedAlbumFolderLabel,
                                createYamlButton, productIdField::getText, this::getMetadataName,
                                rowYamlToGme::setSelectedYamlFile, this::log, this::setWorkflowStatus, taskManager);
                createYamlButton.addEventFilter(ActionEvent.ACTION,
                                event -> validationSupport.initInitialDecoration());
                rowConvertAudio.setOnAudioPrepared(audioFolder -> {
                        File albumFolder = audioFolder.getParentFile();
                        rowConvertAudio.setSelectedAudioFolder(albumFolder);
                        rowCreateYaml.setSelectedAlbumFolder(albumFolder);
                });
                rowExportTonieAudio = new RowExportTonieAudio(stage, selectTonieFileButton, selectedTonieFileLabel,
                                exportTonieAudioButton, tonieAudioExportService, tonieExportDestinationService,
                                this::getAlbumName, this::log, rowConvertAudio::setSelectedAudioFolder,
                                this::setWorkflowStatus, taskManager);
                albumWorkflowContinuation = new AlbumWorkflowContinuation(rowCreateYaml, rowYamlToGme,
                                rowCreateOidTable,
                                rowExportTonieAudio,
                                rowConvertAudio::getSelectedAudioFolder, workflowResolver, this::log,
                                this::getMetadataName, this::setWorkflowStatus);
                rowConvertAudio.setOnSelectedAudioFolder(folder -> {
                        selectedFolderPathLabel.setText(folder.getAbsolutePath());
                        productNameField.setText(albumNameFromFolderName(folder.getName()));
                        albumWorkflowContinuation.updateSelectedFolderControls();
                });
                new RowListGmeProductIds(stage, selectGmeFolderButton, selectedGmeFolderLabel,
                                listGmeProductIdsButton, listGmeProductIdsSpinner, tttoolService, productIdRows,
                                this::log, taskManager);

                ExpandableSubActions exportToniePane = new ExpandableSubActions(
                                "Only export Tonie audio", selectTonieFileButton, selectedTonieFileLabel,
                                exportTonieAudioButton);
                ExpandableSubActions prepAudioPane = new ExpandableSubActions(
                                "Only prep audio", selectAudioFolderButton, selectedAudioFolderLabel,
                                convertAudioButton);
                ExpandableSubActions createYamlPane = new ExpandableSubActions(
                                "Only create YAML", selectAlbumFolderButton, selectedAlbumFolderLabel,
                                createYamlButton);
                ExpandableSubActions createGmePane = new ExpandableSubActions(
                                "Only create GME", selectYamlFileButton, selectedYamlFileLabel,
                                createGmeButton);
                ExpandableSubActions createOidTablePane = new ExpandableSubActions(
                                "Only create OID table", selectOidTableFolderButton, selectedOidTableFolderLabel,
                                createOidTableButton);
                ExpandableSubActions listGmeProductIdsPane = new ExpandableSubActions(
                                "List GME Product IDs", selectGmeFolderButton, selectedGmeFolderLabel,
                                listGmeProductIdsButton, productIdTable, listGmeProductIdsSpinner);
                Accordion workflowPanes = new Accordion(exportToniePane, prepAudioPane, createYamlPane, createGmePane,
                                createOidTablePane,
                                listGmeProductIdsPane);

                workflowStatusLabel = new Label();
                workflowStatusLabel.setMinHeight(20);
                workflowStatusSpinner = new ProgressIndicator();
                workflowStatusSpinner.setPrefSize(16, 16);
                workflowStatusSpinner.setMaxSize(16, 16);
                workflowStatusSpinner.setVisible(false);
                workflowStatusSpinner.setManaged(false);
                HBox workflowStatusRow = new HBox(6, workflowStatusSpinner, workflowStatusLabel);
                VBox buttonBox = new VBox(10, rowInput, runRow, workflowPanes, workflowStatusRow);

                outputArea = new TextArea();
                outputArea.setEditable(false);

                TitledPane logPane = new TitledPane("Logs", outputArea);
                logPane.setExpanded(false);
                logPane.setMaxHeight(Double.MAX_VALUE);

                BorderPane root = new BorderPane();
                root.setPadding(new Insets(10));
                VBox topContent = new VBox(10, title, buttonBox);
                root.setTop(topContent);
                root.setBottom(logPane);
                BorderPane.setMargin(topContent, new Insets(0, 0, 10, 0));

                logPane.expandedProperty().addListener((observable, wasExpanded, isExpanded) -> {
                        if (isExpanded) {
                                root.setBottom(null);
                                root.setCenter(logPane);
                        } else {
                                root.setCenter(null);
                                root.setBottom(logPane);
                        }
                });

                runButton.setOnAction(e -> runTool());
                cancelButton.setOnAction(e -> cancelRunningTasks());
                taskManager.setOnRunningChanged(isRunning -> javafx.application.Platform.runLater(() -> {
                        cancelButton.setVisible(isRunning);
                        cancelButton.setManaged(isRunning);
                }));

                Scene scene = new Scene(root, 500, 600);
                stage.setTitle("TTTool Album Creator");
                stage.setScene(scene);
                stage.show();

                javafx.application.Platform.runLater(root::requestFocus);
        }

        private void runTool() {
                File selectedFolder = rowConvertAudio.getSelectedAudioFolder();
                if (selectedFolder == null) {
                        log("Please select a folder first.");
                        return;
                }

                AlbumFolderWorkflowResolver.WorkflowResolution resolution = workflowResolver.resolve(selectedFolder);
                if (requiresProductId(resolution) && validationSupport.isInvalid()) {
                        validationSupport.initInitialDecoration();
                        log("Please enter a valid product ID.");
                        return;
                }
                switch (resolution.workflow()) {
                        case EXPORT_TONIE_AUDIO -> {
                                File exportFolder;
                                try {
                                        exportFolder = tonieExportDestinationService.createExportFolder(
                                                        resolution.tonieFile(), getAlbumName());
                                } catch (java.io.IOException e) {
                                        log("Could not create Tonie export folder: " + e.getMessage());
                                        return;
                                }
                                rowExportTonieAudio.runToolExportAudio(resolution.tonieFile(), exportFolder,
                                                exportedAlbumFolder -> {
                                                        rowConvertAudio.setSelectedAudioFolder(exportedAlbumFolder);
                                                        rowConvertAudio.runToolCopyAndConvertForExistingAlbum(
                                                                        exportedAlbumFolder,
                                                                        albumWorkflowContinuation::continueFromAudioFolder);
                                                });
                        }
                        case PROCESS_AUDIO -> rowConvertAudio
                                        .runToolCopyAndConvert(albumWorkflowContinuation::continueFromAudioFolder);
                        case EXISTING_ALBUM -> albumWorkflowContinuation.continueFromExistingAlbum(selectedFolder,
                                        resolution.yamlFile());
                        case UNSUPPORTED ->
                                log("Selected folder contains no Tonie file, MP3/OGG files, or audio directory.");
                }
        }

        private TableView<RowListGmeProductIds.ProductIdTableRow> createProductIdTable(
                        ObservableList<RowListGmeProductIds.ProductIdTableRow> productIdRows) {
                TableColumn<RowListGmeProductIds.ProductIdTableRow, String> gmeColumn = new TableColumn<>("gme");
                gmeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().gme()));
                gmeColumn.setPrefWidth(300);

                TableColumn<RowListGmeProductIds.ProductIdTableRow, Integer> productIdColumn = new TableColumn<>(
                                "Product ID");
                productIdColumn.setCellValueFactory(
                                cell -> new SimpleIntegerProperty(cell.getValue().productId()).asObject());
                productIdColumn.setPrefWidth(130);

                TableView<RowListGmeProductIds.ProductIdTableRow> table = new TableView<>(productIdRows);
                table.getColumns().add(gmeColumn);
                table.getColumns().add(productIdColumn);
                table.getSortOrder().add(gmeColumn);
                table.setPlaceholder(new Label("No Product IDs listed yet"));
                table.setPrefHeight(200);
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
                return table;
        }

        private String getAlbumName() {
                return productNameField.getText().trim();
        }

        private String getMetadataName() {
                String productName = getAlbumName();
                if (!appendProductIdToggle.isSelected()) {
                        return productName;
                }

                String productId = productIdField.getText().trim();
                if (productId.isEmpty()) {
                        return productName;
                }
                return (productName.isEmpty() ? "tttoolAlbum" : productName) + "_" + productId;
        }

        private static boolean isValidProductId(String productId) {
                if (productId == null || productId.isBlank()) {
                        return false;
                }
                try {
                        Integer.parseInt(productId);
                        return true;
                } catch (NumberFormatException e) {
                        return false;
                }
        }

        private static boolean requiresProductId(AlbumFolderWorkflowResolver.WorkflowResolution resolution) {
                return resolution.workflow() == AlbumFolderWorkflowResolver.Workflow.EXPORT_TONIE_AUDIO
                                || resolution.workflow() == AlbumFolderWorkflowResolver.Workflow.PROCESS_AUDIO
                                || (resolution.workflow() == AlbumFolderWorkflowResolver.Workflow.EXISTING_ALBUM
                                                && resolution.yamlFile() == null);
        }

        private static String albumNameFromFolderName(String folderName) {
                if (folderName.endsWith("_album")) {
                        return folderName.substring(0, folderName.length() - "_album".length());
                }
                if (folderName.endsWith("_export")) {
                        return folderName.substring(0, folderName.length() - "_export".length());
                }
                return folderName;
        }

        private void log(String message) {
                Runnable appendMessage = () -> outputArea.appendText(message + "\n");
                if (javafx.application.Platform.isFxApplicationThread()) {
                        appendMessage.run();
                } else {
                        javafx.application.Platform.runLater(appendMessage);
                }
        }

        private void cancelRunningTasks() {
                taskManager.cancelAll();
                log("Cancellation requested.");
        }

        private void setWorkflowStatus(String message) {
                Runnable updateStatus = () -> {
                        boolean isRunning = message != null && message.endsWith("...");
                        workflowStatusSpinner.setVisible(isRunning);
                        workflowStatusSpinner.setManaged(isRunning);
                        workflowStatusLabel.setText(message);
                };
                if (javafx.application.Platform.isFxApplicationThread()) {
                        updateStatus.run();
                } else {
                        javafx.application.Platform.runLater(updateStatus);
                }
        }
}
