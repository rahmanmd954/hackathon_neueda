import java.awt.*;
import javax.swing.*;

public class GUIBankingApp extends JFrame {
    private double currentBalance = 2500.00;
    private DefaultListModel<String> transactionListModel;
    private JLabel balanceLabel;
    private JComboBox<String> transactionTypeCombo;
    private JTextField amountField, descriptionField;
    private JPanel recipientPanel;
    private JLabel statusLabel;
    private JComboBox<String> recipientCombo; // Add this line

    public GUIBankingApp() {
        setTitle("SecureBank Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Account Overview Panel
        JPanel accountPanel = new JPanel();
        accountPanel.setBorder(BorderFactory.createTitledBorder("Account Overview"));
        accountPanel.setLayout(new GridLayout(0,1,5,5));
        accountPanel.add(new JLabel("Account Number: ****1234"));
        accountPanel.add(new JLabel("Account Type: Checking"));
        accountPanel.add(new JLabel("Account Holder: John Doe"));
        balanceLabel = new JLabel("$" + String.format("%.2f", currentBalance), SwingConstants.CENTER);
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(24f));
        accountPanel.add(balanceLabel);

        JPanel quickPanel = new JPanel(new GridLayout(1,4,5,5));
        JButton quickDepBtn = new JButton("Deposit $100");
        JButton quickWithBtn = new JButton("Withdraw $50");
        JButton viewStmtBtn = new JButton("View Statement");
        JButton transferBtn = new JButton("Transfer");
        quickPanel.add(quickDepBtn);
        quickPanel.add(quickWithBtn);
        quickPanel.add(viewStmtBtn);
        quickPanel.add(transferBtn);
        accountPanel.add(quickPanel);

        // Transaction Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Make Transaction"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Transaction Type:"), gbc);
        gbc.gridx = 1;
        transactionTypeCombo = new JComboBox<>(new String[]{"Select","Deposit","Withdraw","Transfer"});
        formPanel.add(transactionTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1;
        amountField = new JTextField();
        formPanel.add(amountField, gbc);

        recipientPanel = new JPanel(new BorderLayout());
        recipientPanel.add(new JLabel("Recipient:"), BorderLayout.WEST);
        recipientCombo = new JComboBox<>(new String[]{"Savings", "Checking"});
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

        // Transaction History
        transactionListModel = new DefaultListModel<>();
        transactionListModel.addElement("Jan 15, 2025 - Initial Deposit: +$2,500.00");
        JList<String> transactionList = new JList<>(transactionListModel);

        // Custom cell renderer for coloring
        transactionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String entry = value.toString();
                if (entry.contains("Deposit") || entry.contains("Quick Deposit") || entry.matches(".*[+]\\$.*")) {
                    label.setForeground(new Color(0, 128, 0)); // Green
                } else if (entry.contains("Withdraw") || entry.contains("Quick Withdraw") || entry.matches(".*[-]\\$.*")) {
                    label.setForeground(Color.RED);
                } else {
                    label.setForeground(Color.BLACK);
                }
                return label;
            }
        });

        JScrollPane historyScroll = new JScrollPane(transactionList);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Recent Transactions"));
        historyScroll.setPreferredSize(new Dimension(400,150));

        // Layout
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(10,10));
        cp.add(accountPanel, BorderLayout.NORTH);
        cp.add(formPanel, BorderLayout.CENTER);
        cp.add(historyScroll, BorderLayout.SOUTH);

        // Events
        transactionTypeCombo.addActionListener(e -> {
            boolean isTransfer = "Transfer".equals(transactionTypeCombo.getSelectedItem());
            recipientPanel.setVisible(isTransfer);
            pack();
        });

        processBtn.addActionListener(e -> processTransaction());
        quickDepBtn.addActionListener(e -> quickDeposit());
        quickWithBtn.addActionListener(e -> quickWithdraw());
        viewStmtBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Statement emailed!"));
        transferBtn.addActionListener(e -> {
            transactionTypeCombo.setSelectedItem("Transfer");
            recipientPanel.setVisible(true);
            pack();
        });
    }

    private void processTransaction() {
        String type = (String)transactionTypeCombo.getSelectedItem();
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
        if (("Withdraw".equals(type) || "Transfer".equals(type)) && amount > currentBalance) {
            showStatus("Insufficient funds", Color.RED);
            return;
        }
        if ("Transfer".equals(type) && recipientCombo.getSelectedItem() == null) {
            showStatus("Select recipient", Color.RED);
            return;
        }

        // Update balance
        currentBalance += "Deposit".equals(type) ? amount : -amount;
        balanceLabel.setText("$" + String.format("%.2f", currentBalance));

        // Record transaction
        String desc = descriptionField.getText().trim();
        if (desc.isEmpty()) desc = type;
        String recipient = recipientCombo.getSelectedItem() != null ? recipientCombo.getSelectedItem().toString() : "";
        String entry = String.format("%s - %s%s$%.2f%s",
                new java.text.SimpleDateFormat("MMM d, yyyy HH:mm").format(new java.util.Date()),
                desc,
                "Deposit".equals(type) ? "+" : "-",
                amount,
                "Transfer".equals(type) ? " to " + recipient : "");
        transactionListModel.add(0, entry);
        showStatus("Success!", Color.GREEN);

        // Reset form
        transactionTypeCombo.setSelectedIndex(0);
        amountField.setText("");
        descriptionField.setText("");
        recipientPanel.setVisible(false);
        pack();
    }

    private void quickDeposit() {
        currentBalance += 100;
        balanceLabel.setText("$" + String.format("%.2f", currentBalance));
        transactionListModel.add(0, new java.text.SimpleDateFormat("MMM d, yyyy HH:mm").format(new java.util.Date()) +
                " - Quick Deposit: +$100.00");
        showStatus("Quick deposit successful", Color.GREEN);
        pack();
    }

    private void quickWithdraw() {
        if (currentBalance >= 50) {
            currentBalance -= 50;
            balanceLabel.setText("$" + String.format("%.2f", currentBalance));
            transactionListModel.add(0, new java.text.SimpleDateFormat("MMM d, yyyy HH:mm").format(new java.util.Date()) +
                    " - Quick Withdraw: -$50.00");
            showStatus("Quick withdrawal successful", Color.GREEN);
        } else {
            showStatus("Insufficient funds", Color.RED);
        }
        pack();
    }

    private void showStatus(String msg, Color col) {
        statusLabel.setText(msg);
        statusLabel.setForeground(col);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUIBankingApp::new);
    }
}
