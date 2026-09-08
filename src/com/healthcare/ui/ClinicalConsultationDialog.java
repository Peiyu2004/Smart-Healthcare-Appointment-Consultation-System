package com.healthcare.ui;

import com.healthcare.model.Appointment;
import com.healthcare.model.Consultation;
import com.healthcare.model.User;
import com.healthcare.service.HealthcareOperationService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ClinicalConsultationDialog extends JDialog {
    private final Appointment appointment;
    private final User currentUser;
    private final HealthcareOperationService operationService;

    public ClinicalConsultationDialog(Frame owner, Appointment app, User user, HealthcareOperationService opService) {
        super(owner, "Clinical Consultation - Appointment ID: " + app.getAppointmentId(), true);
        this.appointment = app;
        this.currentUser = user;
        this.operationService = opService;

        setSize(850, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        initComponents();
    }

    private void initComponents() {
        // 1. Resolve Patient Identifier reliably (checks both ID and Name)
        String patientIdentifier = resolvePatientIdentifier();
        List<Consultation> history = operationService.getPatientHistory(patientIdentifier);

        // 2. Patient History Panel
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Patient Clinical History"));

        if (history == null || history.isEmpty()) {
            JLabel firstVisitBanner = new JLabel("FIRST VISIT - NO PRIOR CLINICAL RECORDS FOUND", SwingConstants.CENTER);
            firstVisitBanner.setOpaque(true);
            firstVisitBanner.setBackground(new Color(255, 230, 230));
            firstVisitBanner.setForeground(Color.RED);
            firstVisitBanner.setFont(new Font("Arial", Font.BOLD, 13));
            firstVisitBanner.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            historyPanel.add(firstVisitBanner, BorderLayout.CENTER);
        } else {
            JTextArea historyArea = new JTextArea(5, 50);
            historyArea.setEditable(false);
            historyArea.setLineWrap(true);
            historyArea.setWrapStyleWord(true);
            historyArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

            StringBuilder sb = new StringBuilder();
            for (Consultation c : history) {
                sb.append("----------------------------------------------------------------------------------------\n")
                  .append("Consultation ID: ").append(c.getConsultationId()).append(" | Date: ")
                  .append(c.getRecordDate()).append(" | Attending Doctor: ").append(c.getDoctorName())
                  .append("\n• Diagnosis: ").append(c.getDiagnosis())
                  .append("\n• Clinical Notes: ").append(c.getClinicalNotes());

                String rxText = getPrescriptionSafely(c);
                if (rxText != null && !rxText.trim().isEmpty()) {
                    sb.append("\n• Prescription: ").append(rxText);
                }
                sb.append("\n");
            }
            historyArea.setText(sb.toString());
            historyArea.setCaretPosition(0);

            JScrollPane historyScroll = new JScrollPane(historyArea);
            historyScroll.setPreferredSize(new Dimension(800, 130));
            historyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            historyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            historyPanel.add(historyScroll, BorderLayout.CENTER);
        }

        // 3. Active Consultation Entry Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Active Consultation Entry"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.BOTH;

        JTextArea diagnosisArea = createWrappedTextArea(2, 40);
        JTextArea clinicalNotesArea = createWrappedTextArea(3, 40);
        JTextArea prescriptionArea = createWrappedTextArea(3, 40);

        Consultation existing = operationService.getConsultationByAppointment(appointment.getAppointmentId());
        if (existing != null) {
            diagnosisArea.setText(existing.getDiagnosis());
            clinicalNotesArea.setText(existing.getClinicalNotes());
            prescriptionArea.setText(getPrescriptionSafely(existing));
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2; gbc.weighty = 0.2;
        formPanel.add(new JLabel("Primary Diagnosis*:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8; gbc.weighty = 0.2;
        formPanel.add(new JScrollPane(diagnosisArea), gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2; gbc.weighty = 0.4;
        formPanel.add(new JLabel("Clinical Notes*:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8; gbc.weighty = 0.4;
        formPanel.add(new JScrollPane(clinicalNotesArea), gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2; gbc.weighty = 0.4;
        formPanel.add(new JLabel("Prescription / Advice:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8; gbc.weighty = 0.4;
        formPanel.add(new JScrollPane(prescriptionArea), gbc);

        // 4. Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton submitBtn = HealthcareUI.createStyledButton("Submit & Complete Consultation");
        submitBtn.setFont(new Font("Arial", Font.BOLD, 13));
        JButton dialogCancelBtn = HealthcareUI.createStyledButton("Cancel");
        dialogCancelBtn.setFont(new Font("Arial", Font.PLAIN, 13));

        btnPanel.add(submitBtn);
        btnPanel.add(dialogCancelBtn);

        dialogCancelBtn.addActionListener(e -> dispose());
        submitBtn.addActionListener(e -> {
            String diag = diagnosisArea.getText().trim();
            String notes = clinicalNotesArea.getText().trim();
            String rx = prescriptionArea.getText().trim();

            if (diag.isEmpty() || notes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Validation Error: 'Primary Diagnosis' and 'Clinical Notes' fields are required!",
                        "Validation Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (operationService.saveConsultationRecord(currentUser, appointment.getAppointmentId(), notes, diag, rx)) {
                JOptionPane.showMessageDialog(this, "Consultation record saved successfully and status updated to COMPLETED.");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save record. Ensure you have authorized permissions.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(historyPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    /**
     * Resolves patient identifier across patient ID and name getters.
     */
    private String resolvePatientIdentifier() {
        if (appointment.getPatientId() != null && !appointment.getPatientId().trim().isEmpty()) {
            return appointment.getPatientId().trim();
        }
        if (appointment.getPatientName() != null && !appointment.getPatientName().trim().isEmpty()) {
            return appointment.getPatientName().trim();
        }
        return "";
    }

    private JTextArea createWrappedTextArea(int rows, int cols) {
        JTextArea area = new JTextArea(rows, cols);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return area;
    }

    private String getPrescriptionSafely(Consultation c) {
        if (c == null) return "";
        try {
            java.lang.reflect.Method getPrescriptionMethod = c.getClass().getMethod("getPrescription");
            Object res = getPrescriptionMethod.invoke(c);
            return res != null ? res.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }
}