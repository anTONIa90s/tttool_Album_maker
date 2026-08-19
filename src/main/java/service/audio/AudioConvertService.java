package service.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class AudioConvertService {

    private final Consumer<String> logger;

    public AudioConvertService(Consumer<String> logger) {
        this.logger = logger;
    }

    public void processFolder(File folder) {
        File[] files = folder.listFiles();

        if (files == null)
            return;

        for (File file : files) {

            if (file.isFile()) {
                String name = file.getName().toLowerCase();

                if (name.endsWith(".mp3")) {
                    System.out.println("MP3 OK: " + file.getName());
                    logger.accept("MP3 OK: " + file.getName());
                }

                if (name.endsWith(".ogg")) {
                    System.out.println("Checking OGG: " + file.getName());
                    logger.accept("Checking OGG: " + file.getName());

                    if (needsConversion(file)) {
                        System.out.println(" → Needs conversion");
                        logger.accept(" → Needs conversion");
                        convertOgg(file);
                    } else {
                        System.out.println(" → Already OK");
                        logger.accept(" → Already OK");
                    }
                }

            }
        }
    }

    private boolean needsConversion(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "a:0",
                    "-show_entries", "stream=codec_name,sample_rate,channels,start_time",
                    "-of", "csv=p=0",
                    file.getAbsolutePath());

            pb.redirectErrorStream(true);

            Process process = pb.start();

            String output = new BufferedReader(
                    new InputStreamReader(process.getInputStream())).readLine();

            process.waitFor();

            if (output == null)
                return true;

            // Example: "opus,48000,2"
            String[] parts = output.split(",");

            System.out.println("FFPROBE OUTPUT: " + output);

            String codec = parts[0].trim();
            int sampleRate = Integer.parseInt(parts[1].trim());
            int channels = Integer.parseInt(parts[2].trim());
            double startTime = Double.parseDouble(parts[3].trim());

            System.out.println("Codec: " + codec);
            System.out.println("SampleRate: " + sampleRate);
            System.out.println("Channels: " + channels);
            System.out.println("StartTime: " + startTime);

            boolean isVorbis = codec.equals("vorbis");
            boolean is22050 = sampleRate == 22050;
            boolean isMono = channels == 1;
            boolean isStart0 = Math.abs(startTime) == 0;
            System.out.println("Is Okay?: " + (isVorbis && is22050 && isMono && isStart0));

            return !(isVorbis && is22050 && isMono && isStart0);

        } catch (Exception e) {
            e.printStackTrace();
            logger.accept("Could not inspect audio file " + file.getName() + ": " + e.getMessage());
            return true; // fallback → convert
        }
    }

    private void convertOgg(File file) {
        File parent = file.getParentFile();

        // temp file (same folder)
        File tempFile = new File(parent, file.getName() + ".tmp.ogg");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y", // overwrite without asking
                "-i", file.getAbsolutePath(),
                "-ac", "1",
                "-ar", "22050",
                "-c:a", "libvorbis",
                tempFile.getAbsolutePath());

        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            new BufferedReader(
                    new InputStreamReader(process.getInputStream())).lines().forEach(System.out::println);

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // ✅ replace original file
                if (!file.delete()) {
                    System.out.println("Failed to delete original: " + file.getName());
                    logger.accept("Failed to delete original: " + file.getName());
                    return;
                }

                if (!tempFile.renameTo(file)) {
                    System.out.println("Failed to rename temp file: " + file.getName());
                    logger.accept("Failed to rename temp file: " + file.getName());
                    return;
                }

                System.out.println("Replaced: " + file.getName());
                logger.accept("Replaced: " + file.getName());

            } else {
                System.out.println("Conversion failed: " + file.getName());
                logger.accept("Conversion failed: " + file.getName());
                tempFile.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.accept("Could not convert audio file " + file.getName() + ": " + e.getMessage());
        }
    }
}
