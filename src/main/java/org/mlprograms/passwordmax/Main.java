package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.controller.JsonController;
import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.util.AesCryptographer;

public class Main {

    public static void main(String[] args) {
        final JsonController jsonController = new JsonController();
        addSampleData(jsonController);

        jsonController.loadDataFromJson().forEach(account -> {
            System.out.println("Konto: " + account.getName() + " (Masterpasswort: " + account.getDecryptedPassword() + ")");
            account.getEntries().forEach(entry -> System.out.println(entry.encryptEntry(new AesCryptographer(account.getName()))));
        });
    }

    private static void addSampleData(JsonController jsonController) {
        final Account account = new Account(
                "Max",
                "masterpassword"
        );
        final Entry entry = new Entry(
                "NoteMax",
                "Mein SpringBoot Obsidian Klon.",
                "localhost:8080",
                "Panther3",
                "1234",
                "max.mustermann@gmail.com",
                "Kleiner Hinweis..."
        );

        jsonController.addAccount(account);
        jsonController.addPasswordToAccount(account.getName(), entry);
    }

}