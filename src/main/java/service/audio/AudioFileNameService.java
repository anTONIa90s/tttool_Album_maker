package service.audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Renames all files in a folder to consecutively numbered filenames while
 * preserving their file extensions.
 */
public class AudioFileNameService {

    /**
     * Lists the filename changes that would be made, without changing files.
     */
    public List<RenameOperation> preview(File folder) throws IOException {
        return preview(folder.toPath());
    }

    /**
     * Renames the files in {@code folder}. Call {@link #preview(File)} first
     * when the caller needs to ask the user for confirmation.
     */
    public List<RenameOperation> renameFiles(File folder) throws IOException {
        return renameFiles(folder.toPath());
    }

    public List<RenameOperation> preview(Path folder) throws IOException {
        List<Path> files = getSortedFiles(folder);
        int digits = String.valueOf(files.size()).length();
        List<RenameOperation> operations = new ArrayList<>();

        for (int index = 0; index < files.size(); index++) {
            Path source = files.get(index);
            String targetName = String.format("%0" + digits + "d%s", index + 1, getExtension(source));
            operations.add(new RenameOperation(source, source.resolveSibling(targetName)));
        }

        return List.copyOf(operations);
    }

    public List<RenameOperation> renameFiles(Path folder) throws IOException {
        List<RenameOperation> operations = preview(folder);
        List<Path> temporaryFiles = new ArrayList<>();
        String temporaryPrefix = ".__rename_temp_" + UUID.randomUUID() + "_";

        for (int index = 0; index < operations.size(); index++) {
            RenameOperation operation = operations.get(index);
            Path temporaryFile = operation.source().resolveSibling(
                    temporaryPrefix + (index + 1) + "__" + getExtension(operation.source()));
            Files.move(operation.source(), temporaryFile);
            temporaryFiles.add(temporaryFile);
        }

        for (int index = 0; index < operations.size(); index++) {
            Files.move(temporaryFiles.get(index), operations.get(index).target());
        }

        return operations;
    }

    private List<Path> getSortedFiles(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("That directory does not exist: " + folder);
        }

        try (Stream<Path> paths = Files.list(folder)) {
            return paths
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private String getExtension(Path file) {
        String filename = file.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }

    public record RenameOperation(Path source, Path target) {
    }
}
