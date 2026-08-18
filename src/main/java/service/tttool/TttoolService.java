package service.tttool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class TttoolService {

    // Adjust this if your executable is somewhere else
    private static final String TTTOOL_PATH = "./tools/tttool";

    public String assemble(Path yamlFile) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                TTTOOL_PATH,
                "assemble",
                yamlFile.toAbsolutePath().toString());

        // Combine stdout and stderr
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "TTTOOL exited with code " + exitCode +
                            System.lineSeparator() +
                            output);
        }

        return output.toString();
    }
}