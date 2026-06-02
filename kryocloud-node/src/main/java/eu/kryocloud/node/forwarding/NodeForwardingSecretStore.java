package eu.kryocloud.node.forwarding;

import eu.kryocloud.common.layout.KryoDirectoryLayout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public final class NodeForwardingSecretStore {

    private final Path secretFile;
    private final SecureRandom random = new SecureRandom();

    public NodeForwardingSecretStore() {
        this(KryoDirectoryLayout.ROOT.resolve(".kryocloud").resolve("forwarding.secret"));
    }

    public NodeForwardingSecretStore(Path secretFile) {
        if (secretFile == null) {
            throw new IllegalArgumentException("secretFile must not be null");
        }

        this.secretFile = secretFile.toAbsolutePath().normalize();
    }

    public synchronized String secret() {
        try {
            if (Files.exists(secretFile)) {
                String existing = Files.readString(secretFile).trim();

                if (!existing.isBlank()) {
                    return existing;
                }
            }

            Files.createDirectories(secretFile.getParent());
            String generated = generate();
            Files.writeString(secretFile, generated + System.lineSeparator());
            return generated;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load KryoCloud forwarding secret from " + secretFile, exception);
        }
    }

    public Path secretFile() {
        return secretFile;
    }

    private String generate() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
