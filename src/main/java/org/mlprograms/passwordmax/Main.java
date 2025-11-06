package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.controller.JsonController;
import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;

public class Main {

    public static void main(String[] args) {
        final JsonController jsonController = new JsonController();
        addSampleData(jsonController);

        jsonController.loadDataFromJson().forEach(account -> {
            System.out.println("Konto: " + account.getName() + " (Masterpasswort: " + account.getPassword() + ")");
            account.getEntries().forEach(password ->
                    System.out.println("  - Passwort: " + password  .getPassword() + " (" + password.getUsername() + ")"));
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