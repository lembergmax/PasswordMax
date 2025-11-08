package org.mlprograms.passwordmax.ui;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.persistence.AccountManager;
import org.mlprograms.passwordmax.security.Cryptographer;
import org.mlprograms.passwordmax.security.CryptoUtils;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class PasswordMaxUI {

    private final AccountManager accountManager;
    private Account account;
    private final String masterPassword;

    private JFrame frame;
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

    public PasswordMaxUI(final AccountManager accountManager, final Account account, final String masterPassword) throws Exception {
        this.accountManager = accountManager;
        this.account = account;
        this.masterPassword = masterPassword;

        // Berechne SecretKey
        this.secretKey = cryptoUtils.deriveEncryptionKey(masterPassword, Base64.getDecoder().decode(account.getEncryptionSaltBase64()));

        initUI();
        refreshList();
    }

    private void initUI() {
        frame = new JFrame("PasswordMax");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        final JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left: List
        listModel = new DefaultListModel<>();
        entryList = new JList<>(listModel);
        entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryList.addListSelectionListener(e -> onSelectEntry());

        final JScrollPane listScroll = new JScrollPane(entryList);
        listScroll.setPreferredSize(new Dimension(250, 0));
        root.add(listScroll, BorderLayout.WEST);

        // Center: Details
        final JPanel details = new JPanel(new BorderLayout(8, 8));

        final JPanel fields = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        addLabeled(fields, "Name:", nameField = new JTextField(), gbc, row++);
        addLabeled(fields, "Password:", passwordField = new JPasswordField(), gbc, row++);
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
        final JButton addBtn = new JButton("Hinzufügen");
        final JButton updateBtn = new JButton("Aktualisieren");
        final JButton deleteBtn = new JButton("Löschen");
        final JButton saveBtn = new JButton("Speichern");

        addBtn.addActionListener(this::onAdd);
        updateBtn.addActionListener(this::onUpdate);
        deleteBtn.addActionListener(this::onDelete);
        saveBtn.addActionListener(e -> onSave());

        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(saveBtn);

        root.add(buttons, BorderLayout.SOUTH);

        frame.setContentPane(root);
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
            return;
        }
        final String name = listModel.get(idx);
        final Entry entry = account.getEntries().stream()
                .filter(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (entry == null) {
            clearFields();
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

            nameField.setText(clone.getEntryName());
            passwordField.setText(clone.getEncryptedPassword());
            usernameField.setText(clone.getUsername());
            emailField.setText(clone.getEmail());
            urlField.setText(clone.getUrl());
            descriptionArea.setText(clone.getDescription());
            notesArea.setText(clone.getNotes());

        } catch (final Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Fehler beim Entschlüsseln des Eintrags: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        nameField.setText("");
        passwordField.setText("");
        usernameField.setText("");
        emailField.setText("");
        urlField.setText("");
        descriptionArea.setText("");
        notesArea.setText("");
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
    }

    private void onUpdate(final ActionEvent e) {
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

        accountManager.updateEntry(account, masterPassword, updated);
        refreshList();
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
    }

    private void onSave() {
        try {
            accountManager.saveAccount(account);
            JOptionPane.showMessageDialog(frame, "Account gespeichert.", "Info", JOptionPane.INFORMATION_MESSAGE);
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
