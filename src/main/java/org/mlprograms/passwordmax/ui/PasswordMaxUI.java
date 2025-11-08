package org.mlprograms.passwordmax.ui;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.persistence.AccountManager;
import org.mlprograms.passwordmax.security.Cryptographer;
import org.mlprograms.passwordmax.security.CryptoUtils;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class PasswordMaxUI {

    private final AccountManager accountManager;
    private Account account;
    private String masterPassword;

    private JFrame frame;
    private JPanel cards; // CardLayout root
    private static final String CARD_LOGIN = "login";
    private static final String CARD_REGISTER = "register";
    private static final String CARD_VAULT = "vault";

    private JList<String> entryList;
    private DefaultListModel<String> listModel;
    private JPanel detailsPanel; // shown only when an entry is selected or when creating a new one

    private JTextField nameField;
    private JPasswordField passwordField;
    private JTextField usernameField;
    private JTextField emailField;
    private JTextField urlField;
    private JTextArea descriptionArea;
    private JTextArea notesArea;

    private final CryptoUtils cryptoUtils = new CryptoUtils();
    private final Cryptographer cryptographer = new Cryptographer();
    private SecretKey secretKey;

    // UI state
    private boolean isDirty = false;
    private String originalSelectedEntryName = null;
    private JButton saveChangesBtn;
    private JButton discardChangesBtn;
    private JButton showPasswordBtn;
    private JButton generatePasswordBtn;
    private JButton deleteBtnRef; // reference to the delete button so we can show/hide it
    private JButton addBtnRef; // reference to enable/disable
    private boolean suppressDirty = false;

    public PasswordMaxUI(final AccountManager accountManager) {
        this.accountManager = accountManager;

        initUI();
        // show login by default
    }

    private void initUI() {
        frame = new JFrame("PasswordMax");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 650);
        frame.setLocationRelativeTo(null);

        cards = new JPanel(new CardLayout());
        cards.setBorder(new EmptyBorder(12, 12, 12, 12));

        cards.add(buildLoginPanel(), CARD_LOGIN);
        cards.add(buildRegisterPanel(), CARD_REGISTER);
        cards.add(buildVaultPanel(), CARD_VAULT);

        frame.setContentPane(cards);
    }

    // Build login panel only (separate from registration)
    private JPanel buildLoginPanel() {
        // center a panel with fixed preferred size
        final JPanel outer = new JPanel(new GridBagLayout());
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(420, 220));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), new EmptyBorder(12, 12, 12, 12)));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        final JLabel title = new JLabel("PasswordMax");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        final JLabel subtitle = new JLabel("Bitte anmelden", SwingConstants.CENTER);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        gbc.gridy = row++;
        panel.add(subtitle, gbc);

        gbc.gridwidth = 1;

        final JTextField usernameLogin = new JTextField();
        final JPasswordField masterPw = new JPasswordField();

        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Benutzername:"), gbc);
        gbc.gridx = 1;
        panel.add(usernameLogin, gbc);
        row++;

        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Master-Passwort:"), gbc);
        gbc.gridx = 1;
        panel.add(masterPw, gbc);
        row++;

        final JButton loginBtn = new JButton("Anmelden");
        final JButton toRegisterBtn = new JButton("Neuen Account erstellen");

        loginBtn.setToolTipText("Mit vorhandenem Account anmelden");
        toRegisterBtn.setToolTipText("Zur Registrierung wechseln");

        loginBtn.addActionListener(e -> {
            final String user = usernameLogin.getText().trim();
            final String pw = new String(masterPw.getPassword());
            if (user.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Benutzername und Passwort dürfen nicht leer sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                final Account loaded = accountManager.loadAccount(user);
                if (loaded == null || !user.equalsIgnoreCase(loaded.getUsername())) {
                    JOptionPane.showMessageDialog(frame, "Kein Account mit diesem Benutzernamen gefunden.", "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!cryptoUtils.verifyPassword(pw, loaded.getVerificationHash())) {
                    JOptionPane.showMessageDialog(frame, "Falsches Passwort.", "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                this.account = loaded;
                this.masterPassword = pw;
                this.secretKey = cryptoUtils.deriveEncryptionKey(masterPassword, Base64.getDecoder().decode(account.getEncryptionSaltBase64()));

                // Decrypt full entries list into memory
                try {
                    accountManager.decryptEntries(this.account, this.masterPassword);
                } catch (final Exception dex) {
                    JOptionPane.showMessageDialog(frame, "Fehler beim Entschlüsseln der Einträge: " + dex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Clear credentials from the login fields for security
                usernameLogin.setText("");
                masterPw.setText("");

                switchToVault();

            } catch (final Exception ex) {
                JOptionPane.showMessageDialog(frame, "Fehler beim Laden/Verifizieren des Accounts: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        toRegisterBtn.addActionListener(e -> {
            final CardLayout cl = (CardLayout) (cards.getLayout());
            cl.show(cards, CARD_REGISTER);
        });

        final JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(toRegisterBtn);
        btns.add(loginBtn);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(btns, gbc);

        outer.add(panel);
        return outer;
    }

    // New: separate registration panel
    private JPanel buildRegisterPanel() {
        final JPanel outer = new JPanel(new GridBagLayout());
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(420, 260));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), new EmptyBorder(12, 12, 12, 12)));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        final JLabel title = new JLabel("Account erstellen");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        final JTextField usernameReg = new JTextField();
        final JPasswordField pwReg = new JPasswordField();
        final JPasswordField pwRegConfirm = new JPasswordField();

        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Benutzername:"), gbc);
        gbc.gridx = 1;
        panel.add(usernameReg, gbc);
        row++;

        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Master-Passwort:"), gbc);
        gbc.gridx = 1;
        panel.add(pwReg, gbc);
        row++;

        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Passwort wiederholen:"), gbc);
        gbc.gridx = 1;
        panel.add(pwRegConfirm, gbc);
        row++;

        final JButton createBtn = new JButton("Account erstellen");
        final JButton cancelBtn = new JButton("Zurück zur Anmeldung");

        createBtn.addActionListener(e -> {
            final String user = usernameReg.getText().trim();
            final String pw = new String(pwReg.getPassword());
            final String pw2 = new String(pwRegConfirm.getPassword());
            if (user.isEmpty() || pw.isEmpty() || pw2.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Alle Felder müssen ausgefüllt sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!pw.equals(pw2)) {
                JOptionPane.showMessageDialog(frame, "Passwörter stimmen nicht überein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                final Account created = accountManager.createAccount(user, pw);
                accountManager.saveAccount(created);

                // Inform the user and redirect to login so they can sign in with the new account
                JOptionPane.showMessageDialog(frame, "Account erstellt. Bitte melde dich nun an.", "Info", JOptionPane.INFORMATION_MESSAGE);
                // clear registration fields
                usernameReg.setText("");
                pwReg.setText("");
                pwRegConfirm.setText("");
                final CardLayout cl = (CardLayout) (cards.getLayout());
                cl.show(cards, CARD_LOGIN);
            } catch (final Exception ex) {
                JOptionPane.showMessageDialog(frame, "Fehler beim Erstellen/Speichern des Accounts: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> {
            final CardLayout cl = (CardLayout) (cards.getLayout());
            cl.show(cards, CARD_LOGIN);
        });

        final JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(cancelBtn);
        btns.add(createBtn);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(btns, gbc);

        outer.add(panel);
        return outer;
    }

    // Build the existing vault UI as a panel (slightly refactored from previous code)
    private JPanel buildVaultPanel() {
        final JPanel root = new JPanel(new BorderLayout(12, 12));

        // Left: List
        listModel = new DefaultListModel<>();
        entryList = new JList<>(listModel);
        entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryList.addListSelectionListener(e -> onSelectEntry());

        final JScrollPane listScroll = new JScrollPane(entryList);
        listScroll.setPreferredSize(new Dimension(280, 0));
        // add a top panel for list actions (deselect)
        final JPanel listPanel = new JPanel(new BorderLayout(6,6));
        final JButton deselectBtn = new JButton("Auswahl aufheben");
        deselectBtn.setToolTipText("Hebt die aktuelle Auswahl auf und zeigt keinen Eintrag an");
        deselectBtn.addActionListener(evt -> {
            entryList.clearSelection();
            clearFields();
            originalSelectedEntryName = null;
            detailsPanel.setVisible(false);
            updateActionButtonsVisibility();
        });
        final JPanel listTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listTop.add(deselectBtn);
        listPanel.add(listTop, BorderLayout.NORTH);
        listPanel.add(listScroll, BorderLayout.CENTER);
        root.add(listPanel, BorderLayout.WEST);

        // Center: Details (hidden initially)
        detailsPanel = new JPanel(new BorderLayout(8, 8));
        final JPanel details = detailsPanel; // alias for local building

        final JPanel fields = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        addLabeled(fields, "Name:", nameField = new JTextField(), gbc, row++);

        // Password field with show + generate buttons
        final JPanel pwPanel = new JPanel(new BorderLayout(6, 6));
        passwordField = new JPasswordField();
        pwPanel.add(passwordField, BorderLayout.CENTER);

        final JPanel pwBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        showPasswordBtn = new JButton("Anzeigen");
        showPasswordBtn.setToolTipText("Passwort anzeigen/ausblenden");
        generatePasswordBtn = new JButton("Generieren");
        generatePasswordBtn.setToolTipText("Erzeuge ein neues sicheres Passwort");

        pwBtns.add(showPasswordBtn);
        pwBtns.add(generatePasswordBtn);
        pwPanel.add(pwBtns, BorderLayout.EAST);

        addLabeled(fields, "Password:", pwPanel, gbc, row++);

        addLabeled(fields, "Username:", usernameField = new JTextField(), gbc, row++);
        addLabeled(fields, "E-Mail:", emailField = new JTextField(), gbc, row++);
        addLabeled(fields, "URL:", urlField = new JTextField(), gbc, row++);

        details.add(fields, BorderLayout.NORTH);

        descriptionArea = new JTextArea(4, 20);
        notesArea = new JTextArea(4, 20);

        final JPanel textAreas = new JPanel(new GridLayout(2, 1, 6, 6));
        textAreas.add(wrapWithTitled("Beschreibung", new JScrollPane(descriptionArea)));
        textAreas.add(wrapWithTitled("Notizen", new JScrollPane(notesArea)));

        details.add(textAreas, BorderLayout.CENTER);

        detailsPanel.setVisible(false);
        root.add(detailsPanel, BorderLayout.CENTER);

        // Bottom: Buttons
        final JPanel buttons = new JPanel(new BorderLayout());
        final JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton addBtn = new JButton("Neuen Eintrag hinzufügen");
        addBtnRef = addBtn;
        final JButton deleteBtn = new JButton("Eintrag löschen");
        deleteBtnRef = deleteBtn;
        saveChangesBtn = new JButton("Änderungen speichern");
        saveChangesBtn.setVisible(false);
        discardChangesBtn = new JButton("Änderungen verwerfen");
        discardChangesBtn.setVisible(false);
        final JButton logoutBtn = new JButton("Abmelden");

        addBtn.setToolTipText("Füge einen neuen Eintrag mit den ausgefüllten Feldern hinzu");
        deleteBtn.setToolTipText("Lösche den aktuell ausgewählten Eintrag");
        saveChangesBtn.setToolTipText("Speichere Änderungen an diesem Eintrag");
        logoutBtn.setToolTipText("Vom aktuellen Account abmelden und zurück zur Anmeldung");

        addBtn.addActionListener(this::onAdd);
        deleteBtn.addActionListener(this::onDelete);
        saveChangesBtn.addActionListener(this::onSave);
        discardChangesBtn.addActionListener(e -> onDiscardChanges());
        logoutBtn.addActionListener(e -> onLogout());
        // delete hidden until an existing entry is selected
        deleteBtn.setVisible(false);

        // right side: add/save/delete
        rightButtons.add(addBtn);
        rightButtons.add(saveChangesBtn);
        rightButtons.add(discardChangesBtn);
        rightButtons.add(deleteBtn);

        // left side bottom: logout
        final JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftBottom.add(logoutBtn);

        buttons.add(rightButtons, BorderLayout.EAST);
        buttons.add(leftBottom, BorderLayout.WEST);
        root.add(buttons, BorderLayout.SOUTH);

        // wire show/generate
        showPasswordBtn.addActionListener(e -> toggleShowPassword());
        generatePasswordBtn.addActionListener(e -> passwordField.setText(generatePassword(16)));

        // document listeners for dirty tracking
        final DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { if (!suppressDirty) setDirty(true); }
            @Override
            public void removeUpdate(DocumentEvent e) { if (!suppressDirty) setDirty(true); }
            @Override
            public void changedUpdate(DocumentEvent e) { if (!suppressDirty) setDirty(true); }
        };

        nameField.getDocument().addDocumentListener(docListener);
        passwordField.getDocument().addDocumentListener(docListener);
        usernameField.getDocument().addDocumentListener(docListener);
        emailField.getDocument().addDocumentListener(docListener);
        urlField.getDocument().addDocumentListener(docListener);
        descriptionArea.getDocument().addDocumentListener(docListener);
        notesArea.getDocument().addDocumentListener(docListener);

        return root;
    }

    private void toggleShowPassword() {
        if (passwordField.getEchoChar() == (char) 0) {
            // currently visible -> mask it
            passwordField.setEchoChar('\u2022');
            showPasswordBtn.setText("Anzeigen");
        } else {
            passwordField.setEchoChar((char) 0);
            showPasswordBtn.setText("Verbergen");
        }
    }

    private String generatePassword(final int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*()-_+=";
        final SecureRandom rnd = new SecureRandom();
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void setDirty(final boolean dirty) {
        this.isDirty = dirty;
        updateActionButtonsVisibility();
    }

    private void updateActionButtonsVisibility() {
        // If an existing entry is selected (originalSelectedEntryName != null), show Save/Discard when dirty
        final boolean existingSelected = originalSelectedEntryName != null;
        saveChangesBtn.setVisible(existingSelected && isDirty);
        discardChangesBtn.setVisible(existingSelected && isDirty);
        // Keep Add button always enabled so user can create a new entry even when one is selected
        if (addBtnRef != null) {
            addBtnRef.setEnabled(true);
        }
        if (deleteBtnRef != null) {
            deleteBtnRef.setVisible(existingSelected);
        }
    }

    private void switchToVault() {
        // refresh list and show vault card
        refreshList();
        originalSelectedEntryName = null;
        setDirty(false);
        final CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, CARD_VAULT);
    }

    private JPanel wrapWithTitled(final String title, final Component comp) {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private void addLabeled(final JPanel panel, final String labelText, final JComponent component, final GridBagConstraints gbc, final int row) {
        final GridBagConstraints l = (GridBagConstraints) gbc.clone();
        l.gridy = row;
        l.gridx = 0;
        l.weightx = 0.0;
        final JLabel label = new JLabel(labelText);
        panel.add(label, l);

        final GridBagConstraints c = (GridBagConstraints) gbc.clone();
        c.gridy = row;
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(component, c);
    }

    private void onSelectEntry() {
        final int idx = entryList.getSelectedIndex();
        if (idx < 0) {
            clearFields();
            originalSelectedEntryName = null;
            setDirty(false);
            detailsPanel.setVisible(false);
            return;
        }
        final String name = listModel.get(idx);
        if (account == null || account.getEntries() == null) {
            // No entries in memory — clear and return
            clearFields();
            originalSelectedEntryName = null;
            setDirty(false);
            detailsPanel.setVisible(false);
            return;
        }
        final Entry entry = account.getEntries().stream()
                .filter(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (entry == null) {
            clearFields();
            originalSelectedEntryName = null;
            setDirty(false);
            return;
        }

        try {
            // clone to avoid mutating stored content
            final Entry clone = new Entry(
                    entry.getEntryName(),
                    entry.getEncryptedEntryName(),
                    entry.getEncryptedPassword(),
                    entry.getDescription(),
                    entry.getUrl(),
                    entry.getUsername(),
                    entry.getEmail(),
                    entry.getNotes()
            );

            // suppress dirty while filling fields programmatically
            suppressDirty = true;
            nameField.setText(clone.getEntryName());
            passwordField.setText(clone.getEncryptedPassword());
            passwordField.setEchoChar('\u2022');
            showPasswordBtn.setText("Anzeigen");
            usernameField.setText(clone.getUsername());
            emailField.setText(clone.getEmail());
            urlField.setText(clone.getUrl());
            descriptionArea.setText(clone.getDescription());
            notesArea.setText(clone.getNotes());
            suppressDirty = false;

            originalSelectedEntryName = entry.getEntryName();
            detailsPanel.setVisible(true);
            setDirty(false);
            updateActionButtonsVisibility();

        } catch (final Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Fehler beim Entschlüsseln des Eintrags: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        suppressDirty = true;
        nameField.setText("");
        passwordField.setText("");
        usernameField.setText("");
        emailField.setText("");
        urlField.setText("");
        descriptionArea.setText("");
        notesArea.setText("");
        suppressDirty = false;
        originalSelectedEntryName = null;
        updateActionButtonsVisibility();
    }

    private void onAdd(final ActionEvent e) {
        // If details panel is hidden, show it and prepare for a new entry
        if (detailsPanel == null || !detailsPanel.isVisible() || originalSelectedEntryName != null) {
            detailsPanel.setVisible(true);
            clearFields();
            originalSelectedEntryName = null; // ensure we are in create mode
            updateActionButtonsVisibility();
            nameField.requestFocusInWindow();
            return;
        }

        final String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Name darf nicht leer sein.", "Validierung", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final Entry entry = new Entry(
                name,
                null,
                new String(passwordField.getPassword()),
                descriptionArea.getText(),
                urlField.getText(),
                usernameField.getText(),
                emailField.getText(),
                notesArea.getText()
        );

        accountManager.addEntry(account, masterPassword, entry);
        refreshList();
        clearFields();
        setDirty(false);
        try {
            accountManager.saveAccountEncrypted(account, masterPassword);
        } catch (final Exception ex) {
            JOptionPane.showMessageDialog(frame, "Fehler beim Speichern nach Hinzufügen: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdate(final ActionEvent e) {
        // removed: update button obsolete — kept for compatibility but not used in UI
    }

    private void onDelete(final ActionEvent e) {
        final String nameFieldText = nameField.getText().trim();
        final String targetName = (originalSelectedEntryName != null) ? originalSelectedEntryName : nameFieldText;
        if (targetName == null || targetName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Name darf nicht leer sein.", "Validierung", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int confirm = JOptionPane.showConfirmDialog(frame, "Eintrag wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        final boolean removed = accountManager.removeEntry(account, targetName);
        if (removed) {
            try {
                accountManager.saveAccountEncrypted(account, masterPassword);
            } catch (final Exception ex) {
                JOptionPane.showMessageDialog(frame, "Eintrag entfernt, aber Speichern fehlgeschlagen: " + ex.getMessage(), "Warnung", JOptionPane.WARNING_MESSAGE);
            }
             refreshList();
             clearFields();
             setDirty(false);
         } else {
             JOptionPane.showMessageDialog(frame, "Eintrag nicht gefunden oder konnte nicht entfernt werden.", "Info", JOptionPane.INFORMATION_MESSAGE);
         }
     }

    private void onDiscardChanges() {
        // reload original entry values into fields
        if (originalSelectedEntryName != null) {
            onSelectEntry();
        } else {
            clearFields();
        }
    }

    private void onLogout() {
        // Clear sensitive data and switch to login view
        suppressDirty = true;
        account = null;
        masterPassword = null;
        secretKey = null;
        originalSelectedEntryName = null;
        listModel.clear();
        clearFields();
        if (detailsPanel != null) detailsPanel.setVisible(false);
        setDirty(false);
        suppressDirty = false;

        final CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, CARD_LOGIN);
    }

    private void onSave(final ActionEvent e) {
        onSave();
    }

    private void onSave() {
        try {
            final String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Name darf nicht leer sein.", "Validierung", JOptionPane.WARNING_MESSAGE);
                return;
            }

            final Entry updated = new Entry(
                    name,
                    null,
                    new String(passwordField.getPassword()),
                    descriptionArea.getText(),
                    urlField.getText(),
                    usernameField.getText(),
                    emailField.getText(),
                    notesArea.getText()
            );

            if (originalSelectedEntryName != null) {
                accountManager.updateEntry(account, masterPassword, originalSelectedEntryName, updated);
            } else {
                accountManager.addEntry(account, masterPassword, updated);
            }

            accountManager.saveAccountEncrypted(account, masterPassword);
            refreshList();
            setDirty(false);
            JOptionPane.showMessageDialog(frame, "Änderungen gespeichert.", "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (final Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Fehler beim Speichern: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshList() {
        listModel.clear();
        if (account == null || account.getEntries() == null) return;
        final List<String> names = account.getEntries().stream()
                .map(Entry::getEntryName)
                .filter(n -> n != null)
                .collect(Collectors.toList());
        names.forEach(listModel::addElement);
    }

    public void show() {
        frame.setVisible(true);
    }
}
