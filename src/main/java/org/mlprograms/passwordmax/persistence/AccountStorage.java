package org.mlprograms.passwordmax.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mlprograms.passwordmax.model.AccountData;

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
        final File dir = new File(userHome, PASSWORDMAX_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
            if (System.getProperty(OS_NAME).toLowerCase().contains(WIN)) {
                try {
                    Runtime.getRuntime().exec("attrib +H \"" + dir.getAbsolutePath() + "\"");
                } catch (IOException ignored) {
                }
            }
        }
        return new File(dir, DATA_JSON);
    }

    public void save(final AccountData accountData) throws IOException {
        final File file = getDefaultFile();
        if (file.exists()) {
            throw new IOException("Datei existiert bereits und darf nicht überschrieben werden.");
        }
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(accountData, writer);
        }
    }

    public AccountData load() throws IOException {
        final File file = getDefaultFile();
        if (!file.exists()) {
            throw new IOException("Datei existiert nicht: " + file.getAbsolutePath());
        }
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, AccountData.class);
        }
    }

}
