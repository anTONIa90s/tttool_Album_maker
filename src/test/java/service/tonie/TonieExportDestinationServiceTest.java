package service.tonie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TonieExportDestinationServiceTest {

    @TempDir
    Path temporaryDirectory;

    private final TonieExportDestinationService service = new TonieExportDestinationService();

    @Test
    void usesTheTonieParentFolderNameWhenNoProductNameIsGiven() throws IOException {
        Path parentFolder = Files.createDirectory(temporaryDirectory.resolve("source"));
        File tonieFile = Files.createFile(parentFolder.resolve("tonie")).toFile();

        File exportFolder = service.createExportFolder(tonieFile, "");

        assertEquals(parentFolder.resolve("source_export").toFile(), exportFolder);
        assertTrue(exportFolder.isDirectory());
    }

    @Test
    void usesTheProductNameWhenItIsGiven() throws IOException {
        Path parentFolder = Files.createDirectory(temporaryDirectory.resolve("source"));
        File tonieFile = Files.createFile(parentFolder.resolve("tonie")).toFile();

        File exportFolder = service.createExportFolder(tonieFile, "My Album");

        assertEquals(parentFolder.resolve("My Album_export").toFile(), exportFolder);
        assertTrue(exportFolder.isDirectory());
    }
}
