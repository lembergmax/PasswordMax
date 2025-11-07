package org.mlprograms.passwordmax.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Slf4j
public class FolderController {

    private final String APP_FOLDER_NAME = ".passwordmax";
    private final String USER_HOME_PATH = System.getProperty("user.home");
    private final Path appFolder;

    public FolderController() {
        this.appFolder = Path.of(USER_HOME_PATH, APP_FOLDER_NAME);
    }

    public void createAppFolders() {
        createAppFolder();
        createKeyFolder();
    }

    private void createAppFolder() {
        try {
            if (!Files.exists(appFolder)) {
                Files.createDirectories(appFolder);

                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    final ProcessBuilder processBuilder = new ProcessBuilder("attrib", "+H", appFolder.toString());
                    processBuilder.inheritIO().start();
                }
            }
        } catch (final IOException ioException) {
            log.error("Konnte App-Verzeichnis nicht anlegen: {}", appFolder, ioException);
            throw new RuntimeException("Konnte App-Verzeichnis nicht anlegen", ioException);
        }
    }

    private void createKeyFolder() {
        try {
            final Path keyFolder = appFolder.resolve("keys");
            if (!Files.exists(keyFolder)) {
                Files.createDirectories(keyFolder);
            }
        } catch (final IOException ioException) {
            log.error("Konnte Key-Verzeichnis nicht anlegen", ioException);
            throw new RuntimeException("Konnte Key-Verzeichnis nicht anlegen", ioException);
        }
    }

    public Path getKeyFolder() {
        return appFolder.resolve("keys");
    }

    public void createUserKeyFolder(final String username) {
        try {
            final Path keyFolder = getKeyFolder();
            if (!Files.exists(keyFolder)) {
                Files.createDirectories(keyFolder);
            }
        } catch (final IOException ioException) {
            log.error("Konnte Benutzer-Key-Verzeichnis nicht anlegen: {}", username, ioException);
            throw new RuntimeException("Konnte Benutzer-Key-Verzeichnis nicht anlegen", ioException);
        }
    }

}
