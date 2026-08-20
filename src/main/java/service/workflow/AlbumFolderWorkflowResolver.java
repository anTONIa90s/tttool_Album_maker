package service.workflow;

import service.tonie.TonieAudioExportService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/** Determines which album workflow applies to the contents of a selected folder. */
public class AlbumFolderWorkflowResolver {

    private final TonieAudioExportService tonieAudioExportService;

    public AlbumFolderWorkflowResolver(TonieAudioExportService tonieAudioExportService) {
        this.tonieAudioExportService = tonieAudioExportService;
    }

    /**
     * Resolves workflows in priority order: Tonie export, source-audio processing, then an existing album.
     */
    public WorkflowResolution resolve(File folder) {
        File tonieFile = findTonieFile(folder);
        if (tonieFile != null) {
            return new WorkflowResolution(Workflow.EXPORT_TONIE_AUDIO, tonieFile, null);
        }

        if (containsAudioFiles(folder)) {
            return new WorkflowResolution(Workflow.PROCESS_AUDIO, null, null);
        }

        if (new File(folder, "audio").isDirectory()) {
            return new WorkflowResolution(Workflow.EXISTING_ALBUM, null, findAlbumYamlFile(folder));
        }

        return new WorkflowResolution(Workflow.UNSUPPORTED, null, null);
    }

    /** Returns whether the selected folder is an existing album that already has a main YAML file. */
    public boolean hasExistingAlbumYaml(File folder) {
        return folder != null
                && new File(folder, "audio").isDirectory()
                && findAlbumYamlFile(folder) != null;
    }

    /** Creates the standard {@code <source>/<album-name>/audio} destination for exported Tonie audio. */
    public File createAlbumAudioFolder(File sourceFolder, String albumName) {
        String resolvedAlbumName = albumName == null || albumName.isBlank() ? "tttoolAlbum" : albumName.trim();
        File audioFolder = new File(new File(sourceFolder, resolvedAlbumName), "audio");
        return audioFolder.isDirectory() || audioFolder.mkdirs() ? audioFolder : null;
    }

    private File findTonieFile(File folder) {
        File[] files = folder.listFiles(File::isFile);
        if (files == null) {
            return null;
        }
        return Arrays.stream(files)
                .filter(this::isTonieFile)
                .findFirst()
                .orElse(null);
    }

    private boolean isTonieFile(File file) {
        try {
            tonieAudioExportService.findAudioOffset(file.toPath());
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean containsAudioFiles(File folder) {
        File[] files = folder.listFiles(File::isFile);
        return files != null && Arrays.stream(files).anyMatch(this::hasAudioExtension);
    }

    private boolean hasAudioExtension(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3") || name.endsWith(".ogg");
    }

    private File findAlbumYamlFile(File albumFolder) {
        File[] yamlFiles = albumFolder.listFiles(file -> {
            if (!file.isFile()) {
                return false;
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            return (name.endsWith(".yaml") || name.endsWith(".yml")) && !name.endsWith(".codes.yaml");
        });
        return yamlFiles == null ? null : Arrays.stream(yamlFiles)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .findFirst()
                .orElse(null);
    }

    public enum Workflow {
        EXPORT_TONIE_AUDIO,
        PROCESS_AUDIO,
        EXISTING_ALBUM,
        UNSUPPORTED
    }

    public record WorkflowResolution(Workflow workflow, File tonieFile, File yamlFile) {
    }
}
