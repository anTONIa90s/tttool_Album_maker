package service.tonie;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Extracts independently playable chapter OGG/Opus streams from Tonie audio files. */
public class TonieAudioExportService {

    private static final int LENGTH_PREFIX_BYTES = 4;
    private static final byte[] OGG_CAPTURE_PATTERN = {'O', 'g', 'g', 'S'};
    private static final int OGG_HEADER_BYTES = 27;
    private static final int OGG_CRC_OFFSET = 22;
    private static final int OGG_PAGE_SEQUENCE_OFFSET = 18;
    private static final int OGG_GRANULE_POSITION_OFFSET = 6;
    private static final int OGG_HEADER_TYPE_OFFSET = 5;

    /** Exports one OGG file per chapter using the Tonie parent directory as its base name. */
    public ExportResult export(Path tonieFile, Path outputDirectory) throws IOException {
        return export(tonieFile, outputDirectory, null).getFirst();
    }

    /**
     * Exports one independently playable OGG file per Tonie chapter.
     * The optional title is used only as the safe output filename base.
     */
    public List<ExportResult> export(Path tonieFile, Path outputDirectory, String title) throws IOException {
        validateInput(tonieFile, outputDirectory);
        TonieHeader header = readHeader(tonieFile);
        String filenameBase = filenameBase(tonieFile, title);

        List<OggPage> pages = readOggPages(tonieFile, header.audioOffset());
        List<Integer> chapterStarts = header.chapterPageNumbers().isEmpty()
                ? List.of(0)
                : resolveChapterStarts(pages, header.chapterPageNumbers());

        List<ExportResult> results = new ArrayList<>();
        for (int chapter = 0; chapter < chapterStarts.size(); chapter++) {
            checkCancelled();
            int start = chapterStarts.get(chapter);
            int end = chapter + 1 < chapterStarts.size() ? chapterStarts.get(chapter + 1) : pages.size();
            Path target = outputDirectory.resolve(filenameBase + "-" + String.format("%02d", chapter + 1) + ".ogg");
            writeChapter(target, pages, start, end);
            results.add(new ExportResult(target, Files.size(target), header.audioOffset(), header.audioSha1()));
        }
        return List.copyOf(results);
    }

    /** Returns the position at which the OGG stream begins, after validation. */
    public long findAudioOffset(Path tonieFile) throws IOException {
        return readHeader(tonieFile).audioOffset();
    }

    private TonieHeader readHeader(Path tonieFile) throws IOException {
        if (!Files.isRegularFile(tonieFile)) {
            throw new IOException("Tonie file does not exist: " + tonieFile);
        }
        try (InputStream input = Files.newInputStream(tonieFile)) {
            byte[] lengthBytes = input.readNBytes(LENGTH_PREFIX_BYTES);
            if (lengthBytes.length != LENGTH_PREFIX_BYTES) {
                throw new IOException("File is too short to contain a Tonie header.");
            }
            long headerLength = readBigEndianUint32(lengthBytes, 0);
            long audioOffset = LENGTH_PREFIX_BYTES + headerLength;
            if (audioOffset >= Files.size(tonieFile) || headerLength > Integer.MAX_VALUE) {
                throw new IOException("Header length leaves no audio data in the Tonie file.");
            }

            byte[] protobufHeader = input.readNBytes((int) headerLength);
            if (protobufHeader.length != headerLength) {
                throw new IOException("Tonie header is truncated.");
            }
            byte[] capturePattern = input.readNBytes(OGG_CAPTURE_PATTERN.length);
            if (!Arrays.equals(capturePattern, OGG_CAPTURE_PATTERN)) {
                throw new IOException("The data after the Tonie header is not an OGG stream.");
            }
            return new TonieHeader(audioOffset, parseChapterPageNumbers(protobufHeader), sha1(tonieFile, audioOffset));
        }
    }

    private List<Integer> parseChapterPageNumbers(byte[] protobufHeader) throws IOException {
        List<Integer> chapters = new ArrayList<>();
        int[] position = {0};
        while (position[0] < protobufHeader.length) {
            long tag = readVarint(protobufHeader, position);
            if (tag == 0) {
                break; // the zero-filled field-5 padding starts here
            }
            int fieldNumber = (int) (tag >>> 3);
            int wireType = (int) (tag & 0x07);
            if (wireType == 0) {
                readVarint(protobufHeader, position);
            } else if (wireType == 2) {
                long length = readVarint(protobufHeader, position);
                if (length > protobufHeader.length - position[0]) {
                    throw new IOException("Tonie header contains an invalid length-delimited field.");
                }
                int end = position[0] + (int) length;
                if (fieldNumber == 4) {
                    while (position[0] < end) {
                        long pageNumber = readVarint(protobufHeader, position);
                        if (pageNumber > Integer.MAX_VALUE) {
                            throw new IOException("Chapter page number is too large.");
                        }
                        chapters.add((int) pageNumber);
                    }
                } else {
                    position[0] = end;
                }
            } else {
                throw new IOException("Unsupported Tonie header field type: " + wireType);
            }
        }
        return chapters.stream().distinct().sorted().toList();
    }

    private List<OggPage> readOggPages(Path tonieFile, long audioOffset) throws IOException {
        List<OggPage> pages = new ArrayList<>();
        try (InputStream input = Files.newInputStream(tonieFile)) {
            input.skipNBytes(audioOffset);
            while (true) {
                checkCancelled();
                byte[] header = input.readNBytes(OGG_HEADER_BYTES);
                if (header.length == 0) {
                    break;
                }
                if (header.length != OGG_HEADER_BYTES || !startsWithOggCapturePattern(header)) {
                    throw new IOException("Invalid OGG page in Tonie audio data.");
                }
                int segmentCount = Byte.toUnsignedInt(header[26]);
                byte[] segmentTable = input.readNBytes(segmentCount);
                if (segmentTable.length != segmentCount) {
                    throw new IOException("Truncated OGG segment table.");
                }
                int bodyLength = 0;
                for (byte segmentLength : segmentTable) {
                    bodyLength += Byte.toUnsignedInt(segmentLength);
                }
                byte[] body = input.readNBytes(bodyLength);
                if (body.length != bodyLength) {
                    throw new IOException("Truncated OGG page body.");
                }
                ByteArrayOutputStream page = new ByteArrayOutputStream(OGG_HEADER_BYTES + segmentCount + bodyLength);
                page.writeBytes(header);
                page.writeBytes(segmentTable);
                page.writeBytes(body);
                pages.add(new OggPage(readLittleEndianUint32(header, OGG_PAGE_SEQUENCE_OFFSET), page.toByteArray()));
            }
        }
        if (pages.size() < 3) {
            throw new IOException("OGG stream does not contain enough pages for chapter export.");
        }
        return pages;
    }

    private List<Integer> resolveChapterStarts(List<OggPage> pages, List<Integer> chapterPageNumbers)
            throws IOException {
        List<Integer> starts = new ArrayList<>();
        for (int chapterPageNumber : chapterPageNumbers) {
            int pageIndex = -1;
            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).sequenceNumber() == chapterPageNumber) {
                    pageIndex = i;
                    break;
                }
            }
            if (pageIndex < 0) {
                throw new IOException("Chapter page " + chapterPageNumber + " was not found in the OGG stream.");
            }
            if ((pages.get(pageIndex).bytes()[OGG_HEADER_TYPE_OFFSET] & 0x01) != 0) {
                throw new IOException("Chapter page " + chapterPageNumber + " starts in the middle of an OGG packet.");
            }
            starts.add(pageIndex);
        }
        return starts.stream().distinct().sorted().toList();
    }

    private void writeChapter(Path target, List<OggPage> allPages, int start, int end) throws IOException {
        List<OggPage> chapterPages = new ArrayList<>();
        if (start == 0) {
            chapterPages.addAll(allPages.subList(start, end));
        } else {
            chapterPages.add(allPages.get(0)); // OpusHead
            chapterPages.add(allPages.get(1)); // OpusTags
            chapterPages.addAll(allPages.subList(start, end));
        }
        long granuleBaseline = start == 0 ? 0 : readLittleEndianLong(allPages.get(start - 1).bytes(), OGG_GRANULE_POSITION_OFFSET);

        try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            for (int i = 0; i < chapterPages.size(); i++) {
                checkCancelled();
                byte[] page = chapterPages.get(i).bytes().clone();
                writeLittleEndianUint32(page, OGG_PAGE_SEQUENCE_OFFSET, i);
                if (start > 0 && i >= 2) {
                    long granulePosition = readLittleEndianLong(page, OGG_GRANULE_POSITION_OFFSET);
                    if (granulePosition >= 0) {
                        writeLittleEndianLong(page, OGG_GRANULE_POSITION_OFFSET, Math.max(0, granulePosition - granuleBaseline));
                    }
                }
                page[OGG_HEADER_TYPE_OFFSET] = (byte) (page[OGG_HEADER_TYPE_OFFSET] & ~0x04);
                if (i == chapterPages.size() - 1) {
                    page[OGG_HEADER_TYPE_OFFSET] |= 0x04;
                }
                writeOggChecksum(page);
                output.write(page);
            }
        }
    }

    private void validateInput(Path tonieFile, Path outputDirectory) throws IOException {
        if (!Files.isRegularFile(tonieFile)) {
            throw new IOException("Tonie file does not exist: " + tonieFile);
        }
        if (!Files.isDirectory(outputDirectory)) {
            throw new IOException("Output directory does not exist: " + outputDirectory);
        }
    }

    private String filenameBase(Path tonieFile, String title) {
        Path parent = tonieFile.getParent();
        String defaultPrefix = parent == null || parent.getFileName() == null
                ? tonieFile.getFileName().toString()
                : parent.getFileName().toString();
        String candidate = title == null || title.isBlank() ? defaultPrefix : title.trim();
        String safe = candidate.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").replaceAll("\\s+", " ").trim();
        return safe.isEmpty() ? "tonie-audio" : safe;
    }

    private long readVarint(byte[] data, int[] position) throws IOException {
        long value = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            if (position[0] >= data.length) {
                throw new IOException("Tonie header ends in a protobuf varint.");
            }
            int next = Byte.toUnsignedInt(data[position[0]++]);
            value |= (long) (next & 0x7f) << shift;
            if ((next & 0x80) == 0) {
                return value;
            }
        }
        throw new IOException("Invalid protobuf varint in Tonie header.");
    }

    private String sha1(Path file, long offset) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(file)) {
                input.skipNBytes(offset);
                byte[] buffer = new byte[8192];
                for (int bytesRead; (bytesRead = input.read(buffer)) != -1; ) {
                    checkCancelled();
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest()).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is unavailable", e);
        }
    }

    private static boolean startsWithOggCapturePattern(byte[] page) {
        return page[0] == 'O' && page[1] == 'g' && page[2] == 'g' && page[3] == 'S';
    }

    private void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Tonie audio export cancelled.");
        }
    }

    private static long readBigEndianUint32(byte[] data, int offset) {
        return ((long) Byte.toUnsignedInt(data[offset]) << 24)
                | ((long) Byte.toUnsignedInt(data[offset + 1]) << 16)
                | ((long) Byte.toUnsignedInt(data[offset + 2]) << 8)
                | Byte.toUnsignedInt(data[offset + 3]);
    }

    private static int readLittleEndianUint32(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset])
                | (Byte.toUnsignedInt(data[offset + 1]) << 8)
                | (Byte.toUnsignedInt(data[offset + 2]) << 16)
                | (Byte.toUnsignedInt(data[offset + 3]) << 24);
    }

    private static long readLittleEndianLong(byte[] data, int offset) {
        long result = 0;
        for (int i = 7; i >= 0; i--) {
            result = (result << 8) | Byte.toUnsignedInt(data[offset + i]);
        }
        return result;
    }

    private static void writeLittleEndianUint32(byte[] data, int offset, int value) {
        for (int i = 0; i < Integer.BYTES; i++) {
            data[offset + i] = (byte) (value >>> (i * 8));
        }
    }

    private static void writeLittleEndianLong(byte[] data, int offset, long value) {
        for (int i = 0; i < Long.BYTES; i++) {
            data[offset + i] = (byte) (value >>> (i * 8));
        }
    }

    private static void writeOggChecksum(byte[] page) {
        Arrays.fill(page, OGG_CRC_OFFSET, OGG_CRC_OFFSET + Integer.BYTES, (byte) 0);
        int checksum = 0;
        for (byte value : page) {
            checksum ^= Byte.toUnsignedInt(value) << 24;
            for (int bit = 0; bit < 8; bit++) {
                checksum = (checksum << 1) ^ ((checksum < 0) ? 0x04c11db7 : 0);
            }
        }
        writeLittleEndianUint32(page, OGG_CRC_OFFSET, checksum);
    }

    private record TonieHeader(long audioOffset, List<Integer> chapterPageNumbers, String audioSha1) {
    }

    private record OggPage(int sequenceNumber, byte[] bytes) {
    }

    public record ExportResult(Path exportedFile, long audioLength, long audioOffset, String audioSha1) {
    }
}
