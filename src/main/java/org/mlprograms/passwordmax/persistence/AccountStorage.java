package org.mlprograms.passwordmax.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.mlprograms.passwordmax.model.Account;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
            if (!directory.mkdirs()) {
                System.err.println("Konnte Verzeichnis nicht anlegen: " + directory.getAbsolutePath());
            }

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

    // Save or replace a single account inside the data.json which now stores a list of accounts.
    public synchronized void save(final Account account) throws IOException {
        if (account == null) throw new IllegalArgumentException("account is null");
        final File file = getDefaultFile();

        // Load existing accounts (if any), merge/replace by username, then write full list
        final List<Account> accounts = new ArrayList<>();
        if (file.exists()) {
            try (final FileReader reader = new FileReader(file)) {
                final JsonElement element = JsonParser.parseReader(reader);
                if (element.isJsonArray()) {
                    final Type listType = new TypeToken<List<Account>>() {}.getType();
                    final List<Account> existing = gson.fromJson(element, listType);
                    if (existing != null) accounts.addAll(existing);
                } else if (element.isJsonObject()) {
                    // Backwards compatibility: file contains a single Account object
                    final Account single = gson.fromJson(element, Account.class);
                    if (single != null) accounts.add(single);
                }
            } catch (final JsonParseException | IOException e) {
                // If parsing fails, overwrite file with current account list (do not fail save)
                System.err.println("Fehler beim Lesen der bestehenden data.json, überschreibe: " + e.getMessage());
            }
        }

        // replace account with same username or add
        boolean replaced = false;
        for (int i = 0; i < accounts.size(); i++) {
            final Account a = accounts.get(i);
            if (a.getUsername() != null && a.getUsername().equalsIgnoreCase(account.getUsername())) {
                accounts.set(i, account);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            accounts.add(account);
        }

        // write full list
        try (final FileWriter writer = new FileWriter(file)) {
            gson.toJson(accounts, writer);
        }
    }

    // Load all accounts (returns list) - throws IOException if file missing
    public synchronized List<Account> loadAll() throws IOException {
        final File file = getDefaultFile();
        if (!file.exists()) {
            throw new IOException("Datei existiert nicht: " + file.getAbsolutePath());
        }
        try (final FileReader fileReader = new FileReader(file)) {
            final JsonElement element = JsonParser.parseReader(fileReader);
            if (element.isJsonArray()) {
                final Type listType = new TypeToken<List<Account>>() {}.getType();
                final List<Account> list = gson.fromJson(element, listType);
                return list != null ? list : new ArrayList<>();
            } else if (element.isJsonObject()) {
                final Account single = gson.fromJson(element, Account.class);
                final List<Account> list = new ArrayList<>();
                if (single != null) list.add(single);
                return list;
            } else {
                return new ArrayList<>();
            }
        }
    }

    // Load a single account by username (case-insensitive), returns null if not found
    public synchronized Account load(final String username) throws IOException {
        if (username == null) throw new IllegalArgumentException("username is null");
        final List<Account> list = loadAll();
        for (final Account a : list) {
            if (a.getUsername() != null && a.getUsername().equalsIgnoreCase(username)) {
                return a;
            }
        }
        return null;
    }

    // Delete the storage file
    public synchronized void deleteFile() throws IOException {
        final File file = getDefaultFile();
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Datei konnte nicht gelöscht werden: " + file.getAbsolutePath());
            }
        }
    }

}
