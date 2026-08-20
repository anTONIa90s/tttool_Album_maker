package service.tttool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import tiptoieditor.ui.WorkflowTaskManager;

public class TttoolService {

    // Adjust this if your executable is somewhere else
    private static final String TTTOOL_PATH = "./tools/tttool";
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("(?m)^Product ID:\\s*(\\d+)\\s*$");
    private final WorkflowTaskManager taskManager;

    public TttoolService() {
        this(null);
    }

    public TttoolService(WorkflowTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public String assemble(Path yamlFile) throws IOException, InterruptedException {
        return runTttool("assemble", yamlFile);
    }

    /**
     * Reads the product ID reported by {@code tttool info} for one GME file.
     */
    public int getProductId(Path gmeFile) throws IOException, InterruptedException {
        String output = runTttool("info", gmeFile);
        return parseProductId(output, gmeFile);
    }

    static int parseProductId(String output, Path gmeFile) throws IOException {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(output);
        if (!matcher.find()) {
            throw new IOException("tttool did not report a Product ID for " + gmeFile);
        }
        return Integer.parseInt(matcher.group(1));
    }

    /**
     * Finds GME files below {@code directory}, including subdirectories, and reads their product IDs.
     * A problem with one file is returned with that file instead of stopping the entire scan.
     */
    public List<ProductIdResult> listProductIds(Path directory) throws IOException, InterruptedException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Directory does not exist: " + directory);
        }

        List<Path> gmeFiles = findGmeFiles(directory);

        List<ProductIdResult> results = new ArrayList<>();
        for (Path gmeFile : gmeFiles) {
            try {
                results.add(ProductIdResult.success(gmeFile, getProductId(gmeFile)));
            } catch (IOException | RuntimeException e) {
                results.add(ProductIdResult.failure(gmeFile, e.getMessage()));
            }
        }
        return results;
    }

    static List<Path> findGmeFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(TttoolService::isGmeFile)
                    .sorted(Comparator.comparing(path -> path.toAbsolutePath().toString()))
                    .toList();
        }
    }

    private static boolean isGmeFile(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.toLowerCase(Locale.ROOT).endsWith(".gme");
    }

    private String runTttool(String command, Path inputFile) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                TTTOOL_PATH,
                command,
                inputFile.toAbsolutePath().toString());

        // Combine stdout and stderr
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        if (taskManager != null) {
            taskManager.register(process);
        }

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } finally {
            if (taskManager != null) {
                taskManager.unregister(process);
            }
        }

        if (exitCode != 0) {
            throw new RuntimeException(
                    "TTTOOL exited with code " + exitCode +
                            System.lineSeparator() +
                            output);
        }

        return output.toString();
    }

    public record ProductIdResult(Path gmeFile, Integer productId, String error) {

        public static ProductIdResult success(Path gmeFile, int productId) {
            return new ProductIdResult(gmeFile, productId, null);
        }

        public static ProductIdResult failure(Path gmeFile, String error) {
            return new ProductIdResult(gmeFile, null, error == null ? "Unknown error" : error);
        }

        public boolean isSuccess() {
            return productId != null;
        }
    }
}
