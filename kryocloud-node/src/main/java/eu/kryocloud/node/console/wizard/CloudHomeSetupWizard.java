package eu.kryocloud.node.console.wizard;

import eu.kryocloud.common.layout.KryoDirectoryLayout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public final class CloudHomeSetupWizard {

    private final Scanner scanner = new Scanner(System.in);

    public void run() {
        KryoDirectoryLayout.bootstrap();

        if (KryoDirectoryLayout.hasExternalHome()) {
            return;
        }

        if (KryoDirectoryLayout.hasPersistedHomePointer()) {
            return;
        }

        printHeader();

        Path selectedRoot = askDirectory("KryoCloud data directory", KryoDirectoryLayout.suggestedHomeDirectory());

        KryoDirectoryLayout.use(selectedRoot);
        KryoDirectoryLayout.persistHomePointer(selectedRoot);
        KryoDirectoryLayout.ensureNodeDirectories();

        printDone(selectedRoot);
    }

    private void printHeader() {
        System.out.println();
        System.out.println("❄ KryoCloud storage setup");
        System.out.println("Choose where KryoCloud should store config, groups, templates, versions, tmp, static services and .jdk runtimes.");
        System.out.println("The " + KryoDirectoryLayout.homePointer().getFileName() + " pointer will be written to: " + KryoDirectoryLayout.homePointer().toAbsolutePath().normalize());
        System.out.println();
    }

    private void printDone(Path root) {
        System.out.println();
        System.out.println("✓ KryoCloud data directory saved.");
        System.out.println("  Home:    " + root.toAbsolutePath().normalize());
        System.out.println("  Pointer: " + KryoDirectoryLayout.homePointer().toAbsolutePath().normalize());
        System.out.println();
    }

    private Path askDirectory(String question, Path fallback) {
        while (true) {
            System.out.print(question + " [" + fallback + "]: ");
            String input = scanner.nextLine();
            String selected = input == null || input.isBlank() ? fallback.toString() : input.trim();
            Path path = Path.of(selected).toAbsolutePath().normalize();

            if (validDirectory(path)) {
                return path;
            }

            System.out.println("Directory must not point to an existing regular file.");
        }
    }

    private boolean validDirectory(Path path) {
        if (!Files.exists(path)) {
            return true;
        }

        return Files.isDirectory(path);
    }
}
