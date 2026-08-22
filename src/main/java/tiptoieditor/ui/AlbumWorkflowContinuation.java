package tiptoieditor.ui;

import service.workflow.AlbumFolderWorkflowResolver;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Continues the UI workflow after audio preparation or when opening an existing album. */
public class AlbumWorkflowContinuation {

    private final RowCreateYaml rowCreateYaml;
    private final RowYamlToGme rowYamlToGme;
    private final RowCreateOidTable rowCreateOidTable;
    private final RowExportTonieAudio rowExportTonieAudio;
    private final Supplier<File> selectedFolderSupplier;
    private final AlbumFolderWorkflowResolver workflowResolver;
    private final Consumer<String> logger;
    private final Supplier<String> productNameSupplier;
    private final Consumer<String> statusUpdater;

    public AlbumWorkflowContinuation(RowCreateYaml rowCreateYaml, RowYamlToGme rowYamlToGme,
                                     RowCreateOidTable rowCreateOidTable,
                                     RowExportTonieAudio rowExportTonieAudio,
                                     Supplier<File> selectedFolderSupplier,
                                     AlbumFolderWorkflowResolver workflowResolver, Consumer<String> logger,
                                     Supplier<String> productNameSupplier, Consumer<String> statusUpdater) {
        this.rowCreateYaml = rowCreateYaml;
        this.rowYamlToGme = rowYamlToGme;
        this.rowCreateOidTable = rowCreateOidTable;
        this.rowExportTonieAudio = rowExportTonieAudio;
        this.selectedFolderSupplier = selectedFolderSupplier;
        this.workflowResolver = workflowResolver;
        this.logger = logger;
        this.productNameSupplier = productNameSupplier;
        this.statusUpdater = statusUpdater;
    }

    public void continueFromAudioFolder(File audioFolder) {
        continueFromAudioFolder(audioFolder, null);
    }

    /** Continues the prepared-audio workflow and invokes {@code onComplete} after the OID table is created. */
    public void continueFromAudioFolder(File audioFolder, Runnable onComplete) {
        continueFromAudioFolder(audioFolder, onComplete, null);
    }

    /** Continues prepared audio and reports a failed workflow step to {@code onFailure}. */
    public void continueFromAudioFolder(File audioFolder, Runnable onComplete, Consumer<String> onFailure) {
        File albumFolder = audioFolder.getParentFile();
        rowCreateYaml.setSelectedAlbumFolder(albumFolder);
        logger.accept("Selected album folder: " + albumFolder.getAbsolutePath());

        rowCreateYaml.runToolCreateYaml(generatedYamlFile ->
                rowYamlToGme.runToolCreateGmeFromYaml(assembledYaml -> runOidTableCreation(onComplete, onFailure),
                        onFailure), onFailure);
    }

    public void continueFromExistingAlbum(File albumFolder, File yamlFile) {
        continueFromExistingAlbum(albumFolder, yamlFile, null);
    }

    /** Continues an existing album and invokes {@code onComplete} after the OID table is created. */
    public void continueFromExistingAlbum(File albumFolder, File yamlFile, Runnable onComplete) {
        continueFromExistingAlbum(albumFolder, yamlFile, onComplete, null);
    }

    /** Continues an existing album and reports a failed workflow step to {@code onFailure}. */
    public void continueFromExistingAlbum(File albumFolder, File yamlFile, Runnable onComplete,
            Consumer<String> onFailure) {
        rowCreateYaml.setSelectedAlbumFolder(albumFolder);
        if (yamlFile != null) {
            rowYamlToGme.setSelectedYamlFile(yamlFile);
            logger.accept("Using existing YAML: " + yamlFile.getAbsolutePath());
            rowYamlToGme.runToolCreateGmeFromYaml(yaml -> runOidTableCreation(onComplete, onFailure), onFailure);
            return;
        }

        rowCreateYaml.runToolCreateYaml(generatedYamlFile ->
                rowYamlToGme.runToolCreateGmeFromYaml(assembledYaml -> runOidTableCreation(onComplete, onFailure),
                        onFailure), onFailure);
    }

    private void runOidTableCreation(Runnable onComplete, Consumer<String> onFailure) {
        rowCreateOidTable.runToolCreateOidTable(
                () -> {
                    statusUpdater.accept("Done! Album created for "
                            + productNameSupplier.get().replaceFirst("_album$", ""));
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }, onFailure);
    }

    /** Synchronizes the sub-workflow controls with the folder selected for the Run workflow. */
    public void updateSelectedFolderControls() {
        File selectedFolder = selectedFolderSupplier.get();
        if (selectedFolder != null) {
            rowCreateYaml.setSelectedAlbumFolder(selectedFolder);
            AlbumFolderWorkflowResolver.WorkflowResolution resolution = workflowResolver.resolve(selectedFolder);
            if (resolution.workflow() == AlbumFolderWorkflowResolver.Workflow.EXPORT_TONIE_AUDIO) {
                rowExportTonieAudio.setSelectedTonieFile(resolution.tonieFile());
            }
            if (resolution.yamlFile() != null) {
                rowYamlToGme.setSelectedYamlFile(resolution.yamlFile());
                rowCreateOidTable.setSelectedAlbumFolder(selectedFolder);
            }
        }
    }
}
