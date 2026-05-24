package eu.kryocloud.wrapper.instance.runtime;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public final class JdkArchiveExtractor {

    public void extract(Path archive, Path targetDirectory) throws Exception {
        if (archive == null) {
            throw new IllegalArgumentException("archive must not be null");
        }

        if (targetDirectory == null) {
            throw new IllegalArgumentException("targetDirectory must not be null");
        }

        Files.createDirectories(targetDirectory);

        String fileName = archive.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".zip") || fileName.endsWith(".zip.download")) {
            extractZip(archive, targetDirectory);
            return;
        }

        extractTarGz(archive, targetDirectory);
    }

    public Path findJavaHome(Path extractedDirectory, String executableName) throws Exception {
        if (extractedDirectory == null) {
            throw new IllegalArgumentException("extractedDirectory must not be null");
        }

        if (executableName == null || executableName.isBlank()) {
            throw new IllegalArgumentException("executableName must not be blank");
        }

        try (var paths = Files.walk(extractedDirectory, 4, FileVisitOption.FOLLOW_LINKS)) {
            return paths.filter(Files::isDirectory).filter(path -> Files.exists(path.resolve("bin").resolve(executableName))).min(Comparator.comparingInt(path -> extractedDirectory.relativize(path).getNameCount())).orElseThrow(() -> new IllegalStateException("Extracted archive does not contain bin/" + executableName));
        }
    }

    public void deleteRecursively(Path directory) throws Exception {
        if (directory == null) {
            return;
        }

        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private void extractZip(Path archive, Path targetDirectory) throws Exception {
        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(archive))) {
            for (var entry = zipInput.getNextEntry(); entry != null; entry = zipInput.getNextEntry()) {
                Path destination = destination(targetDirectory, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }

                Files.createDirectories(destination.getParent());
                Files.copy(zipInput, destination);
            }
        }
    }

    private void extractTarGz(Path archive, Path targetDirectory) throws Exception {
        try (InputStream fileInput = Files.newInputStream(archive); GZIPInputStream gzipInput = new GZIPInputStream(fileInput); BufferedInputStream input = new BufferedInputStream(gzipInput)) {
            byte[] header = new byte[512];

            while (true) {
                int read = input.readNBytes(header, 0, header.length);

                if (read < header.length) {
                    return;
                }

                if (emptyBlock(header)) {
                    return;
                }

                String name = tarString(header, 0, 100);
                String prefix = tarString(header, 345, 155);
                String fullName = prefix.isBlank() ? name : prefix + "/" + name;
                long size = tarOctal(header, 124, 12);
                int mode = (int) tarOctal(header, 100, 8);
                char type = (char) header[156];
                Path destination = destination(targetDirectory, fullName);

                if (type == '5') {
                    Files.createDirectories(destination);
                    skipPadding(input, size);
                    continue;
                }

                if (type == '2') {
                    createSymlink(destination, tarString(header, 157, 100));
                    skipPadding(input, size);
                    continue;
                }

                if (type == '0' || type == '\0') {
                    Files.createDirectories(destination.getParent());
                    Files.copy(new LimitedInputStream(input, size), destination);
                    applyMode(destination, mode);
                    skipPadding(input, size);
                    continue;
                }

                skip(input, size);
                skipPadding(input, size);
            }
        }
    }

    private Path destination(Path targetDirectory, String entryName) {
        Path destination = targetDirectory.resolve(entryName).normalize();

        if (!destination.startsWith(targetDirectory.normalize())) {
            throw new IllegalStateException("Archive entry escapes target directory: " + entryName);
        }

        return destination;
    }

    private void createSymlink(Path destination, String linkTarget) throws Exception {
        if (linkTarget == null || linkTarget.isBlank()) {
            return;
        }

        Files.createDirectories(destination.getParent());
        Files.deleteIfExists(destination);

        try {
            Files.createSymbolicLink(destination, Path.of(linkTarget));
        } catch (UnsupportedOperationException | SecurityException exception) {
            return;
        }
    }

    private void applyMode(Path destination, int mode) {
        try {
            Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.OTHERS_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);

            if ((mode & 0100) != 0) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
            }

            if ((mode & 0010) != 0) {
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
            }

            if ((mode & 0001) != 0) {
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            }

            Files.setPosixFilePermissions(destination, permissions);
        } catch (Exception exception) {
            return;
        }
    }

    private String tarString(byte[] header, int offset, int length) {
        int end = offset;

        while (end < offset + length && header[end] != 0) {
            end++;
        }

        return new String(header, offset, end - offset).trim();
    }

    private long tarOctal(byte[] header, int offset, int length) {
        String value = tarString(header, offset, length);

        if (value.isBlank()) {
            return 0L;
        }

        return Long.parseLong(value.replaceAll("[^0-7]", ""), 8);
    }

    private boolean emptyBlock(byte[] block) {
        for (byte value : block) {
            if (value != 0) {
                return false;
            }
        }

        return true;
    }

    private void skipPadding(InputStream input, long size) throws Exception {
        long padding = (512L - (size % 512L)) % 512L;
        skip(input, padding);
    }

    private void skip(InputStream input, long bytes) throws Exception {
        long remaining = bytes;

        while (remaining > 0) {
            long skipped = input.skip(remaining);

            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }

            if (input.read() < 0) {
                return;
            }

            remaining--;
        }
    }

    private static final class LimitedInputStream extends InputStream {

        private final InputStream source;
        private long remaining;

        private LimitedInputStream(InputStream source, long remaining) {
            this.source = source;
            this.remaining = remaining;
        }

        @Override
        public int read() throws java.io.IOException {
            if (remaining <= 0) {
                return -1;
            }

            int value = source.read();

            if (value >= 0) {
                remaining--;
            }

            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws java.io.IOException {
            if (remaining <= 0) {
                return -1;
            }

            int requested = (int) Math.min(length, remaining);
            int read = source.read(buffer, offset, requested);

            if (read > 0) {
                remaining -= read;
            }

            return read;
        }
    }
}
