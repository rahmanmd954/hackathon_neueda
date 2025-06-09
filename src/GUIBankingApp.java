import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * A tiny two‑account demo (Checking + Savings).
 * – Switch between accounts with the combo in the upper‑right.
 * – Deposit / Withdraw act on the active account.
 * – Transfer moves money from the active account to the other one.
 */
public class GUIBankingApp extends JFrame {
    // ---------- simple data layer ----------
    private final Map<String, Double> balances = new HashMap<>(); // accountName -> balance
    private String currentAccount = "Checking";

    // ---------- ui state ----------
    private DefaultListModel<String> transactionListModel;
    private JLabel balanceLabel, accountTypeLabel;
    private JComboBox<String> transactionTypeCombo;
    private JTextField amountField, descriptionField;
    private JPanel recipientPanel;
    private JComboBox<String> recipientCombo; // destination for transfers
    private JLabel statusLabel;
    private JComboBox<String> accountSwitchBox; // top‑right selector

    public GUIBankingApp() {
        // Show password dialog before anything else
        showPasswordDialog();

        // seed balances
        balances.put("Checking", 2500.00);
        balances.put("Savings", 1000.00);

        setTitle("SecureBank Pro – Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ------------------------------------------- UI BUILD -------------------------------------------
    private void initComponents() {
        /* ------------- account overview ------------- */
        JPanel accountPanel = new JPanel();
        accountPanel.setBorder(BorderFactory.createTitledBorder("Account Overview"));
        accountPanel.setLayout(new GridLayout(0, 1, 5, 5));
        accountPanel.add(new JLabel("Account Number: ****1234"));
        accountTypeLabel = new JLabel("Account Type: " + currentAccount);
        accountPanel.add(accountTypeLabel);
        accountPanel.add(new JLabel("Account Holder: John Doe"));
        balanceLabel = new JLabel("$" + fmt(balances.get(currentAccount)), SwingConstants.CENTER);
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(24f));
        accountPanel.add(balanceLabel);

        // quick actions row
        JPanel quickPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JButton quickDepBtn = new JButton("Deposit $100");
        JButton quickWithBtn = new JButton("Withdraw $50");
        JButton viewStmtBtn = new JButton("View Statement");
        JButton transferBtn = new JButton("Transfer");
        quickPanel.add(quickDepBtn);
        quickPanel.add(quickWithBtn);
        quickPanel.add(viewStmtBtn);
        quickPanel.add(transferBtn);
        accountPanel.add(quickPanel);

        /* ------------- transaction form ------------- */
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Make Transaction"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Transaction Type:"), gbc);
        gbc.gridx = 1;
        transactionTypeCombo = new JComboBox<>(new String[]{"Select", "Deposit", "Withdraw", "Transfer"});
        formPanel.add(transactionTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1;
        amountField = new JTextField();
        formPanel.add(amountField, gbc);

        // recipient (only visible when Transfer)
        recipientPanel = new JPanel(new BorderLayout());
        recipientPanel.add(new JLabel("Recipient:"), BorderLayout.WEST);
        recipientCombo = new JComboBox<>(new String[]{"Checking", "Savings"});
        recipientPanel.add(recipientCombo, BorderLayout.CENTER);
        recipientPanel.setVisible(false);
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        formPanel.add(recipientPanel, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        descriptionField = new JTextField();
        formPanel.add(descriptionField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JButton processBtn = new JButton("Process");
        formPanel.add(processBtn, gbc);

        gbc.gridy++;
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        formPanel.add(statusLabel, gbc);

        /* ------------- history list ------------- */
        transactionListModel = new DefaultListModel<>();
        transactionListModel.addElement("Jan 15, 2025 - Initial Deposit (Checking): +$2,500.00");
        JList<String> transactionList = new JList<>(transactionListModel);
        transactionList.setCellRenderer(createColorRenderer());
        JScrollPane historyScroll = new JScrollPane(transactionList);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Recent Transactions"));
        historyScroll.setPreferredSize(new Dimension(420, 150));

        /* ------------- account switch (upper‑right) ------------- */
        accountSwitchBox = new JComboBox<>(new String[]{"Checking", "Savings"});
        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(accountPanel, BorderLayout.CENTER);
        JPanel switchHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        switchHolder.add(new JLabel("View:"));
        switchHolder.add(accountSwitchBox);
        northWrapper.add(switchHolder, BorderLayout.EAST);

        /* ------------- frame layout ------------- */
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(10, 10));
        cp.add(northWrapper, BorderLayout.NORTH);
        cp.add(formPanel, BorderLayout.CENTER);
        cp.add(historyScroll, BorderLayout.SOUTH);

        /* ------------- listeners ------------- */
        // show / hide recipient combo based on type
        transactionTypeCombo.addActionListener(e -> {
            boolean isTransfer = "Transfer".equals(transactionTypeCombo.getSelectedItem());
            recipientPanel.setVisible(isTransfer);
            if (isTransfer) refreshRecipientOptions();
            pack();
        });

        // process
        processBtn.addActionListener(e -> processTransaction());
        quickDepBtn.addActionListener(e -> quickDeposit());
        quickWithBtn.addActionListener(e -> quickWithdraw());
        viewStmtBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Statement emailed! (demo)"));
        transferBtn.addActionListener(e -> {
            transactionTypeCombo.setSelectedItem("Transfer");
            recipientPanel.setVisible(true);
            refreshRecipientOptions();
            pack();
        });

        // switch active account
        accountSwitchBox.addActionListener(e -> {
            currentAccount = (String) accountSwitchBox.getSelectedItem();
            updateOverview();
            refreshRecipientOptions();
        });
    }

    // -------------------- actions --------------------
    private void processTransaction() {
        String type = (String) transactionTypeCombo.getSelectedItem();
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText());
        } catch (NumberFormatException ex) {
            showStatus("Invalid amount", Color.RED);
            return;
        }
        if ("Select".equals(type)) {
            showStatus("Select type", Color.RED);
            return;
        }
        if (amount <= 0) {
            showStatus("Enter positive amount", Color.RED);
            return;
        }
        double curBal = balances.get(currentAccount);

        if (("Withdraw".equals(type) || "Transfer".equals(type)) && amount > curBal) {
            showStatus("Insufficient funds", Color.RED);
            return;
        }
        // transfer needs a different account chosen
        if ("Transfer".equals(type) && recipientCombo.getSelectedItem() == null) {
            showStatus("Select recipient", Color.RED);
            return;
        }

        /* ----- apply ----- */
        if ("Deposit".equals(type)) {
            balances.put(currentAccount, curBal + amount);
        } else if ("Withdraw".equals(type)) {
            balances.put(currentAccount, curBal - amount);
        } else { // Transfer
            String dest = (String) recipientCombo.getSelectedItem();
            balances.put(currentAccount, curBal - amount);
            balances.put(dest, balances.get(dest) + amount);
        }
        updateOverview();

        /* ----- record ----- */
        String desc = descriptionField.getText().trim();
        if (desc.isEmpty()) desc = type;
        String recipient = "Transfer".equals(type) ? " -> " + recipientCombo.getSelectedItem() : "";
        String entry = String.format("%s - %s (%s): %s$%.2f%s", now(), desc, currentAccount,
                ("Deposit".equals(type) ? "+" : "-"), amount, recipient);
        transactionListModel.add(0, entry);
        showStatus("Success!", new Color(0, 128, 0));

        // reset form
        transactionTypeCombo.setSelectedIndex(0);
        amountField.setText("");
        descriptionField.setText("");
        recipientPanel.setVisible(false);
        pack();
    }

    private void quickDeposit() {
        balances.put(currentAccount, balances.get(currentAccount) + 100);
        updateOverview();
        transactionListModel.add(0, now() + " - Quick Deposit (" + currentAccount + "): +$100.00");
        showStatus("Quick deposit successful", new Color(0, 128, 0));
        pack();
    }

    private void quickWithdraw() {
        double curBal = balances.get(currentAccount);
        if (curBal >= 50) {
            balances.put(currentAccount, curBal - 50);
            updateOverview();
            transactionListModel.add(0, now() + " - Quick Withdraw (" + currentAccount + "): -$50.00");
            showStatus("Quick withdrawal successful", new Color(0, 128, 0));
        } else {
            showStatus("Insufficient funds", Color.RED);
        }
        pack();
    }

    // -------------------- helpers --------------------
    private void updateOverview() {
        balanceLabel.setText("$" + fmt(balances.get(currentAccount)));
        accountTypeLabel.setText("Account Type: " + currentAccount);
    }

    private void refreshRecipientOptions() {
        recipientCombo.removeAllItems();
        for (String acc : balances.keySet()) if (!acc.equals(currentAccount)) recipientCombo.addItem(acc);
    }

    private void showStatus(String msg, Color col) {
        statusLabel.setText(msg);
        statusLabel.setForeground(col);
    }

    private static String now() {
        return new java.text.SimpleDateFormat("MMM d, yyyy HH:mm").format(new java.util.Date());
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    // custom renderer to color entries based on type
    private static ListCellRenderer<? super String> createColorRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String entry = value.toString();
                if (entry.contains("+$")) {          // any credit
                    label.setForeground(new Color(0,128,0));
                } else if (entry.contains("-$")) {   // any debit
                    label.setForeground(Color.RED);
                } else {
                    label.setForeground(Color.BLACK);
                }
                return label;
            }
        };
    }

    // --- Username and Password dialog shown at startup ---
    private void showPasswordDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField userField = new JTextField();
        JPasswordField pwdField = new JPasswordField();
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(pwdField);

        while (true) {
            int option = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Login",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );
            if (option == JOptionPane.OK_OPTION) {
                String username = userField.getText().trim();
                String password = new String(pwdField.getPassword());
                if ("john doe".equalsIgnoreCase(username) && "1234".equals(password)) {
                    break;
                } else {
                    JOptionPane.showMessageDialog(null, "Incorrect username or password. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                    userField.setText("");
                    pwdField.setText("");
                }
            } else {
                System.exit(0); // User cancelled
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUIBankingApp::new);
    }
}
