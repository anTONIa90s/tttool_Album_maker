package service.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.tonie.TonieAudioExportService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlbumFolderWorkflowResolverTest {

    @TempDir
    Path temporaryDirectory;

    private final AlbumFolderWorkflowResolver resolver =
            new AlbumFolderWorkflowResolver(new TonieAudioExportService());

    @Test
    void prioritizesTonieExportOverAudioProcessing() throws IOException {
        Path sourceFolder = Files.createDirectory(temporaryDirectory.resolve("source"));
        Path tonieFile = sourceFolder.resolve("tonie");
        Files.write(tonieFile, new byte[]{0, 0, 0, 0, 'O', 'g', 'g', 'S'});
        Files.createFile(sourceFolder.resolve("chapter.mp3"));

        AlbumFolderWorkflowResolver.WorkflowResolution result = resolver.resolve(sourceFolder.toFile());

        assertEquals(AlbumFolderWorkflowResolver.Workflow.EXPORT_TONIE_AUDIO, result.workflow());
        assertEquals(tonieFile.toFile(), result.tonieFile());
    }

    @Test
    void identifiesSourceAudioFilesForProcessing() throws IOException {
        Path sourceFolder = Files.createDirectory(temporaryDirectory.resolve("source"));
        Files.createFile(sourceFolder.resolve("chapter.OGG"));

        assertEquals(AlbumFolderWorkflowResolver.Workflow.PROCESS_AUDIO,
                resolver.resolve(sourceFolder.toFile()).workflow());
    }

    @Test
    void identifiesExistingAlbumAndIgnoresTheCodesYamlFile() throws IOException {
        Path albumFolder = Files.createDirectory(temporaryDirectory.resolve("album"));
        Files.createDirectory(albumFolder.resolve("audio"));
        Files.createFile(albumFolder.resolve("album.codes.yaml"));
        Path yamlFile = Files.createFile(albumFolder.resolve("album.yaml"));

        AlbumFolderWorkflowResolver.WorkflowResolution result = resolver.resolve(albumFolder.toFile());

        assertEquals(AlbumFolderWorkflowResolver.Workflow.EXISTING_ALBUM, result.workflow());
        assertEquals(yamlFile.toFile(), result.yamlFile());
        assertTrue(resolver.hasExistingAlbumYaml(albumFolder.toFile()));
    }

    @Test
    void marksFoldersWithoutSupportedContentAsUnsupported() throws IOException {
        Path folder = Files.createDirectory(temporaryDirectory.resolve("empty"));

        assertEquals(AlbumFolderWorkflowResolver.Workflow.UNSUPPORTED, resolver.resolve(folder.toFile()).workflow());
        assertFalse(resolver.hasExistingAlbumYaml(folder.toFile()));
    }
}
