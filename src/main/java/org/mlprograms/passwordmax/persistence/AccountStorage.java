package org.mlprograms.passwordmax.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mlprograms.passwordmax.model.Account;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class AccountStorage {

    public final String USER_HOME = "user.home";
    public final String PASSWORDMAX_FOLDER = ".passwordmax";
    public final String OS_NAME = "os.name";
    public final String WIN = "win";
    public final String DATA_JSON = "data.json";
    private final Gson gson;

    public AccountStorage() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public File getDefaultFile() {
        final String userHome = System.getProperty(USER_HOME);
        final File directory = new File(userHome, PASSWORDMAX_FOLDER);

        if (!directory.exists()) {
            directory.mkdirs();

            if (System.getProperty(OS_NAME).toLowerCase().contains(WIN)) {
                try {
                    Runtime.getRuntime().exec("attrib +H \"" + directory.getAbsolutePath() + "\"");
                } catch (final IOException ioException) {
                    System.err.println("Fehler beim Setzen des Hidden-Attributs für das Verzeichnis: " + ioException.getMessage());
                }
            }
        }

        return new File(directory, DATA_JSON);
    }

    public void save(final Account account) throws IOException {
        final File file = getDefaultFile();
        // Allow overwriting the existing file when saving updates to the account
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(account, writer);
        }
    }

    public Account load() throws IOException {
        final File file = getDefaultFile();
        if (!file.exists()) {
            throw new IOException("Datei existiert nicht: " + file.getAbsolutePath());
        }
        try (final FileReader fileReader = new FileReader(file)) {
            return gson.fromJson(fileReader, Account.class);
        }
    }

}
