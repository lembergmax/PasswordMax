package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.persistence.AccountManager;
import org.mlprograms.passwordmax.ui.PasswordMaxUI;

public final class Main {

    public static void main(final String[] args) throws Exception {
        final AccountManager accountManager = new AccountManager();

        // Starte GUI, die Login und Kontoerstellung bereitstellt
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                new PasswordMaxUI(accountManager).show();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

}
