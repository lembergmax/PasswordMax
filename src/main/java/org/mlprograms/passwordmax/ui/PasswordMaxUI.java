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
    private static final String CARD_VAULT = "vault";

    private JList<String> entryList;
    private DefaultListModel<String> listModel;

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
    private JButton showPasswordBtn;
    private JButton generatePasswordBtn;
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
        cards.add(buildVaultPanel(), CARD_VAULT);

        frame.setContentPane(cards);
    }

    // Build login/create panel
    private JPanel buildLoginPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
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

        final JLabel subtitle = new JLabel("Anmelden oder neuen Account erstellen", SwingConstants.CENTER);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        gbc.gridy = row++;
        panel.add(subtitle, gbc);

        gbc.gridwidth = 1;

        final JTextField usernameLogin = new JTextField();
        final JPasswordField masterPw = new JPasswordField();
        final JPasswordField masterPwConfirm = new JPasswordField();

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

        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Passwort wiederholen (bei Erstellung):"), gbc);
        gbc.gridx = 1;
        panel.add(masterPwConfirm, gbc);
        row++;

        final JLabel hint = new JLabel("Hinweis: Das Master-Passwort wird nicht aufbewahrt. Merke es dir gut.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(hint, gbc);
        gbc.gridwidth = 1;

        final JButton loginBtn = new JButton("Anmelden");
        final JButton createBtn = new JButton("Account erstellen");

        loginBtn.setToolTipText("Mit vorhandener Account-Datei anmelden");
        createBtn.setToolTipText("Neuen Account erstellen und lokal speichern");

        loginBtn.addActionListener(e -> {
            final String user = usernameLogin.getText().trim();
            final String pw = new String(masterPw.getPassword());
            if (user.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Benutzername und Passwort dürfen nicht leer sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                final Account loaded = accountManager.loadAccount();
                if (!user.equalsIgnoreCase(loaded.getUsername())) {
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

                switchToVault();

            } catch (final Exception ex) {
                JOptionPane.showMessageDialog(frame, "Fehler beim Laden/Verifizieren des Accounts: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        createBtn.addActionListener(e -> {
            final String user = usernameLogin.getText().trim();
            final String pw = new String(masterPw.getPassword());
            final String pw2 = new String(masterPwConfirm.getPassword());
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
                this.account = created;
                this.masterPassword = pw;
                this.secretKey = cryptoUtils.deriveEncryptionKey(masterPassword, Base64.getDecoder().decode(account.getEncryptionSaltBase64()));

                JOptionPane.showMessageDialog(frame, "Account erstellt.", "Info", JOptionPane.INFORMATION_MESSAGE);
                switchToVault();
            } catch (final Exception ex) {
                JOptionPane.showMessageDialog(frame, "Fehler beim Erstellen/ Speichern des Accounts: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        final JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(loginBtn);
        btns.add(createBtn);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(btns, gbc);

        return panel;
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
        root.add(listScroll, BorderLayout.WEST);

        // Center: Details
        final JPanel details = new JPanel(new BorderLayout(8, 8));

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

        root.add(details, BorderLayout.CENTER);

        // Bottom: Buttons
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton addBtn = new JButton("Neuen Eintrag hinzufügen");
        final JButton deleteBtn = new JButton("Eintrag löschen");
        saveChangesBtn = new JButton("Änderungen speichern");
        saveChangesBtn.setVisible(false);

        addBtn.setToolTipText("Füge einen neuen Eintrag mit den ausgefüllten Feldern hinzu");
        deleteBtn.setToolTipText("Lösche den aktuell ausgewählten Eintrag");
        saveChangesBtn.setToolTipText("Speichere Änderungen an diesem Eintrag");

        addBtn.addActionListener(this::onAdd);
        deleteBtn.addActionListener(this::onDelete);
        saveChangesBtn.addActionListener(this::onSave);

        buttons.add(addBtn);
        buttons.add(saveChangesBtn);
        buttons.add(deleteBtn);

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
        saveChangesBtn.setVisible(dirty);
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
            return;
        }
        final String name = listModel.get(idx);
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
            // clone to avoid mutating stored encrypted content
            final Entry clone = new Entry(
                    entry.getEntryName(),
                    entry.getEncryptedPassword(),
                    entry.getDescription(),
                    entry.getUrl(),
                    entry.getUsername(),
                    entry.getEmail(),
                    entry.getNotes()
            );
            clone.decrypt(secretKey, "user:1234".getBytes(), cryptographer);

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
            setDirty(false);

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
    }

    private void onAdd(final ActionEvent e) {
        final String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Name darf nicht leer sein.", "Validierung", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final Entry entry = new Entry(
                name,
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
    }

    private void onUpdate(final ActionEvent e) {
        // removed: update button obsolete — kept for compatibility but not used in UI
    }

    private void onDelete(final ActionEvent e) {
        final String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Name darf nicht leer sein.", "Validierung", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int confirm = JOptionPane.showConfirmDialog(frame, "Eintrag wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        accountManager.removeEntry(account, name);
        refreshList();
        clearFields();
        originalSelectedEntryName = null;
        setDirty(false);
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

            accountManager.saveAccount(account);
            refreshList();
            setDirty(false);
            JOptionPane.showMessageDialog(frame, "Änderungen gespeichert.", "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (final Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Fehler beim Speichern: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshList() {
        final List<String> names = account.getEntries().stream()
                .map(Entry::getEntryName)
                .filter(n -> n != null)
                .collect(Collectors.toList());

        listModel.clear();
        names.forEach(listModel::addElement);
    }

    public void show() {
        frame.setVisible(true);
    }
}
