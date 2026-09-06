package com.healthcare.ui;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class HealthcareUI extends JFrame {
    private final UserService userService = new UserService();
    private final AppointmentService appointmentService = new AppointmentService();
    private User currentUser = null;

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // UI Components
    private JTextField loginEmailField;
    private JPasswordField loginPassField;
    private JTable slotsTable;
    private JTable appointmentsTable;
    private DefaultTableModel slotsModel;
    private DefaultTableModel appointmentsModel;

    public HealthcareUI() {
        setTitle("Smart Healthcare Appointment System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createDashboardPanel(), "DASHBOARD");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Smart Healthcare System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));

        loginEmailField = new JTextField(15);
        loginPassField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(loginEmailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password/Hash:"), gbc);
        gbc.gridx = 1;
        panel.add(loginPassField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            String email = loginEmailField.getText().trim();
            String pass = new String(loginPassField.getPassword()).trim();
            User user = userService.authenticate(email, pass);
            if (user != null) {
                currentUser = user;
                refreshDashboardData();
                cardLayout.show(mainPanel, "DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Top bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutBtn = new JButton("Logout");
        topPanel.add(logoutBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        logoutBtn.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(mainPanel, "LOGIN");
        });

        // Center Tabs
        JTabbedPane tabbedPane = new JTabbedPane();

        // Slots Tab
        JPanel slotsPanel = new JPanel(new BorderLayout());
        slotsModel = new DefaultTableModel(new String[]{"Slot ID", "Doctor ID", "Date", "Time", "Mode"}, 0);
        slotsTable = new JTable(slotsModel);
        slotsPanel.add(new JScrollPane(slotsTable), BorderLayout.CENTER);

        JButton bookBtn = new JButton("Book Selected Slot");
        slotsPanel.add(bookBtn, BorderLayout.SOUTH);
        tabbedPane.addTab("Available Slots", slotsPanel);

        bookBtn.addActionListener(e -> {
            int row = slotsTable.getSelectedRow();
            if (row != -1) {
                String slotId = (String) slotsModel.getValueAt(row, 0);
                String doctorId = (String) slotsModel.getValueAt(row, 1);
                String reason = JOptionPane.showInputDialog(this, "Enter Reason for Visit:");
                if (reason != null && !reason.trim().isEmpty()) {
                    Appointment app = appointmentService.bookAppointment(currentUser.getUserId(), doctorId, slotId, reason);
                    if (app != null) {
                        JOptionPane.showMessageDialog(this, "Appointment Booked! ID: " + app.getAppointmentId());
                        refreshDashboardData();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a slot from the table.");
            }
        });

        // Appointments Tab
        JPanel appPanel = new JPanel(new BorderLayout());
        appointmentsModel = new DefaultTableModel(new String[]{"App ID", "Doctor ID", "Patient ID", "Status", "Reason"}, 0);
        appointmentsTable = new JTable(appointmentsModel);
        appPanel.add(new JScrollPane(appointmentsTable), BorderLayout.CENTER);

        JButton cancelBtn = new JButton("Cancel Selected Appointment");
        appPanel.add(cancelBtn, BorderLayout.SOUTH);
        tabbedPane.addTab("My Appointments", appPanel);

        cancelBtn.addActionListener(e -> {
            int row = appointmentsTable.getSelectedRow();
            if (row != -1) {
                String appId = (String) appointmentsModel.getValueAt(row, 0);
                String reason = JOptionPane.showInputDialog(this, "Cancellation Reason:");
                if (reason != null && !reason.trim().isEmpty()) {
                    if (appointmentService.cancelAppointment(appId, reason)) {
                        JOptionPane.showMessageDialog(this, "Appointment Cancelled.");
                        refreshDashboardData();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an appointment.");
            }
        });

        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private void refreshDashboardData() {
        // Refresh Slots Table
        slotsModel.setRowCount(0);
        List<ScheduleSlot> slots = appointmentService.getAvailableSlots();
        for (ScheduleSlot s : slots) {
            slotsModel.addRow(new Object[]{s.getSlotId(), s.getDoctorId(), s.getSlotDate(), s.getStartTime() + " - " + s.getEndTime(), s.getMode()});
        }

        // Refresh Appointments Table
        appointmentsModel.setRowCount(0);
        List<Appointment> apps;
        if ("DOCTOR".equalsIgnoreCase(currentUser.getRoleLabel())) {
            apps = appointmentService.getAppointmentsByDoctor(currentUser.getUserId());
        } else {
            apps = appointmentService.getAppointmentsByPatient(currentUser.getUserId());
        }
        for (Appointment a : apps) {
            appointmentsModel.addRow(new Object[]{a.getAppointmentId(), a.getDoctorId(), a.getPatientId(), a.getStatus(), a.getReasonForVisit()});
        }
    }

    public void start() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}