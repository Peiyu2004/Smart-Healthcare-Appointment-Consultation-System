package com.healthcare.ui;

import com.healthcare.service.ReportService;
import com.healthcare.ui.HealthcareUI;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class GenerateReportDialog extends JDialog {

    private JComboBox<String> startYearBox;
    private JComboBox<String> startMonthBox;
    private JComboBox<String> startDayBox;

    private JComboBox<String> endYearBox;
    private JComboBox<String> endMonthBox;
    private JComboBox<String> endDayBox;

    private JComboBox<String> categoryBox;

    public GenerateReportDialog(Frame parent) {
        super(parent, "Generate Analytics & Operational Report", true);
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Main input panel with border padding
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Report Parameters"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Date selection dropdown components
        String[] years = { "2025", "2026", "2027" };
        String[] months = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };
        String[] days = new String[31];
        for (int i = 1; i <= 31; i++) {
            days[i - 1] = String.format("%02d", i);
        }

        // Start Date Picker Panel
        startYearBox = new JComboBox<>(years); startYearBox.setSelectedItem("2026");
        startMonthBox = new JComboBox<>(months); startMonthBox.setSelectedItem("01");
        startDayBox = new JComboBox<>(days); startDayBox.setSelectedItem("01");

        JPanel startDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        startDatePanel.add(startYearBox); startDatePanel.add(new JLabel("-"));
        startDatePanel.add(startMonthBox); startDatePanel.add(new JLabel("-"));
        startDatePanel.add(startDayBox);

        // End Date Picker Panel
        endYearBox = new JComboBox<>(years); endYearBox.setSelectedItem("2026");
        endMonthBox = new JComboBox<>(months); endMonthBox.setSelectedItem("12");
        endDayBox = new JComboBox<>(days); endDayBox.setSelectedItem("31");

        JPanel endDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        endDatePanel.add(endYearBox); endDatePanel.add(new JLabel("-"));
        endDatePanel.add(endMonthBox); endDatePanel.add(new JLabel("-"));
        endDatePanel.add(endDayBox);

        // Category Selection
        String[] categories = {
                "Appointment Status Report",
                "Doctor Performance & Workload",
                "Patient Demographic & Visit History",
                "Revenue & Consultation Fees Summary"
        };
        categoryBox = new JComboBox<>(categories);

        // GridBag Assembly
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Start Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        inputPanel.add(startDatePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("End Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        inputPanel.add(endDatePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Report Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        inputPanel.add(categoryBox, gbc);

        // Button Control Bar
        JButton generateBtn = HealthcareUI.createStyledButton("Generate Report");
        generateBtn.setFont(new Font("Arial", Font.BOLD, 13));

        JButton cancelBtn = HealthcareUI.createStyledButton("Cancel");
        cancelBtn.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.add(generateBtn);
        buttonPanel.add(cancelBtn);

        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        generateBtn.addActionListener(e -> generateReport());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void generateReport() {
        try {
            String startDateStr = startYearBox.getSelectedItem() + "-" + startMonthBox.getSelectedItem() + "-" + startDayBox.getSelectedItem();
            String endDateStr = endYearBox.getSelectedItem() + "-" + endMonthBox.getSelectedItem() + "-" + endDayBox.getSelectedItem();

            LocalDate start = LocalDate.parse(startDateStr);
            LocalDate end = LocalDate.parse(endDateStr);
            String category = (String) categoryBox.getSelectedItem();

            if (end.isBefore(start)) {
                JOptionPane.showMessageDialog(this,
                        "End date cannot be before start date.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            ReportService reportService = new ReportService();
            String result = reportService.generateReport(start, end, category);

            // Display result in a dialog using UI-consistent styling
            JTextArea textArea = new JTextArea(result, 15, 50);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Generated Summary"));

            JOptionPane.showMessageDialog(this,
                    scrollPane,
                    "Report Overview - " + category,
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error generating report: " + ex.getMessage(),
                    "Execution Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}