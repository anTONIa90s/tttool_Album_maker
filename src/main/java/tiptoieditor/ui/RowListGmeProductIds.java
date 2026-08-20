package tiptoieditor.ui;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import service.tttool.TttoolService;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Controls for recursively listing the product IDs in a folder of GME files. */
public class RowListGmeProductIds {

    private final Label selectedFolderLabel;
    private final TttoolService tttoolService;
    private final Consumer<String> logger;
    private final ObservableList<ProductIdTableRow> productIdRows;
    private final ProgressIndicator spinner;
    private final WorkflowTaskManager taskManager;
    private File selectedFolder;

    public RowListGmeProductIds(Stage stage, Button selectFolderButton, Label selectedFolderLabel,
                                Button listProductIdsButton, ProgressIndicator spinner, TttoolService tttoolService,
                                ObservableList<ProductIdTableRow> productIdRows, Consumer<String> logger,
                                WorkflowTaskManager taskManager) {
        this.selectedFolderLabel = selectedFolderLabel;
        this.tttoolService = tttoolService;
        this.productIdRows = productIdRows;
        this.spinner = spinner;
        this.logger = logger;
        this.taskManager = taskManager;
        selectFolderButton.setOnAction(event -> selectFolder(stage));
        listProductIdsButton.setOnAction(event -> listProductIds());
    }

    private void selectFolder(Stage stage) {
        File folder = FolderSelectionDialog.chooseFolder(stage, selectedFolder);
        if (folder != null) {
            selectedFolder = folder;
            selectedFolderLabel.setText(folder.getName());
            logger.accept("Selected GME folder: " + folder.getAbsolutePath());
        }
    }

    private void listProductIds() {
        if (selectedFolder == null) {
            logger.accept("Please select a folder containing GME files first.");
            return;
        }

        Path folder = selectedFolder.toPath();
        logger.accept("Scanning GME files in: " + folder.toAbsolutePath());
        productIdRows.clear();
        spinner.setVisible(true);
        taskManager.start("tttool-gme-product-ids", () -> {
            try {
                List<TttoolService.ProductIdResult> results = tttoolService.listProductIds(folder);
                List<ProductIdTableRow> tableRows = new ArrayList<>();
                if (results.isEmpty()) {
                    logger.accept("No GME files found.");
                    return;
                }

                for (TttoolService.ProductIdResult result : results) {
                    String relativeFile = folder.relativize(result.gmeFile()).toString();
                    if (result.isSuccess()) {
                        logger.accept(relativeFile + " -> Product ID: " + result.productId());
                        tableRows.add(new ProductIdTableRow(relativeFile, result.productId()));
                    } else {
                        logger.accept(relativeFile + " -> Could not read Product ID: " + result.error());
                    }
                }
                Platform.runLater(() -> productIdRows.setAll(tableRows));
                logger.accept("Listed product IDs for " + results.size() + " GME file(s).");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.accept("Product ID scan was interrupted.");
            } catch (Exception e) {
                logger.accept("Could not list GME product IDs: " + e.getMessage());
            } finally {
                Platform.runLater(() -> spinner.setVisible(false));
            }
        });
    }

    public record ProductIdTableRow(String gme, int productId) {
    }
}
