package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.persistence.AccountManager;
import org.mlprograms.passwordmax.ui.PasswordMaxUI;

import java.util.List;

public final class Main {

    public static void main(final String[] args) throws Exception {
        final AccountManager accountManager = new AccountManager();

        final String masterPassword = "Masterpasswort";

        // Erstelle neuen Account (nur in-memory). Um dauerhaft zu speichern, rufe saveAccount auf.
        final Account account = accountManager.createAccount("max", masterPassword);

        addSampleData(account, masterPassword, accountManager);

        // Speichere Account in die Standard-Datei
        accountManager.saveAccount(account);

        // Füge weitere Einträge zu dem bestehenden Account hinzu (Batch)
        final Entry more1 = new Entry("Forum", "forumPass", "Forum Account", "https://forum.example", "maxForum", "", "");
        final Entry more2 = new Entry("E-Mail Konto", "anotherPass", "Doppelter Name Test", "", "", "alt@example.com", "");
        // more2 hat denselben Namen wie entry1 in addSampleData -> wird übersprungen
        accountManager.addEntries(account, masterPassword, List.of(more1, more2));

        // Speichere die Änderungen
        accountManager.saveAccount(account);

        // Starte GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                new PasswordMaxUI(accountManager, account, masterPassword).show();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void addSampleData(final Account account, final String masterPassword, final AccountManager accountManager) {
        final Entry entry1 = new Entry(
                "E-Mail Konto",
                "12345!@#$%",
                "Mein Haupt E-Mail Konto",
                "",
                "",
                "max.mustermann@gmail.com",
                ""
        );

        final Entry entry2 = new Entry(
                "Social Media",
                "pa$$w0rd",
                "Mein Social Media Konto",
                "https://www.socialmedia.com",
                "maxmustermann",
                "",
                "Hier sind meine Social Media Zugangsdaten."
        );

        accountManager.addEntry(account, masterPassword, entry1);
        accountManager.addEntry(account, masterPassword, entry2);
    }

}
