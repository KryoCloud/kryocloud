package eu.kryocloud.launcher.download;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Downloader {

    public Path download(String url, Path target) throws Exception {
        if (Files.exists(target) && Files.size(target) > 0) {
            return target;
        }

        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }

        Path temporary = target.resolveSibling(target.getFileName() + ".part");

        System.out.println("Downloading " + target.getFileName());

        try (InputStream input = URI.create(url).toURL().openStream()) {
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
}
