package com.healthcare.ui;

import com.healthcare.model.Administrator;
import com.healthcare.model.Appointment;
import com.healthcare.model.Doctor;
import com.healthcare.model.Patient;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.HealthcareOperationService;
import com.healthcare.service.StatusTrackingService;
import com.healthcare.service.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Queue;

public class HealthcareUI extends JFrame {
    private final UserService userService = new UserService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final StatusTrackingService statusService = new StatusTrackingService();
    private final HealthcareOperationService operationService = new HealthcareOperationService();

    private User currentUser = null;

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // UI Components
    private JTextField loginEmailField;
    private JPasswordField loginPassField;
    private JLabel userInfoLabel;

    private JTable slotsTable;
    private JTable appointmentsTable;
    private JTable queueTable;

    private DefaultTableModel slotsModel;
    private DefaultTableModel appointmentsModel;
    private DefaultTableModel queueModel;

    private JButton bookBtn;
    private JButton addSlotBtn;
    private JButton updateSlotBtn;
    private JButton deleteSlotBtn;
    private JButton manageUsersBtn;
    private JTabbedPane tabbedPane;
    private JPanel queuePanel;
    
    // Admin Queue Filter
    private JComboBox<String> queueDoctorFilterBox;
    private JPanel queueHeaderPanel;

    public HealthcareUI() {
        // Globally remove focus border rectangle for all buttons
        UIManager.put("Button.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("Button.select", new Color(180, 205, 235));

        setTitle("Smart Healthcare Appointment & Consultation System");
        setSize(1000, 700);
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
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Smart Healthcare System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        loginEmailField = new JTextField(18);
        loginPassField = new JPasswordField(18);

        JButton loginButton = createStyledButton("Login");
        JButton registerButton = createStyledButton("Register");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(loginEmailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(loginPassField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        gbc.gridy = 4;
        panel.add(registerButton, gbc);

        loginButton.addActionListener(e -> {
            String email = loginEmailField.getText().trim();
            String pass = new String(loginPassField.getPassword()).trim();

            if (email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Warning",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            User user = userService.authenticate(email, pass);
            if (user != null) {
                currentUser = user;
                applyRolePermissions();
                refreshDashboardData();
                cardLayout.show(mainPanel, "DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerButton.addActionListener(e -> showRegisterDialog());

        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Header Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        userInfoLabel = new JLabel("Logged in as: User");
        userInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton profileBtn = createStyledButton("My Profile");
        manageUsersBtn = createStyledButton("Manage Users");
        JButton logoutBtn = createStyledButton("Logout");
        topButtons.add(profileBtn);
        topButtons.add(manageUsersBtn);
        topButtons.add(logoutBtn);
        topPanel.add(userInfoLabel, BorderLayout.WEST);
        topPanel.add(topButtons, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        profileBtn.addActionListener(e -> showProfileDialog());
        manageUsersBtn.addActionListener(e -> showManageUsersDialog());

        logoutBtn.addActionListener(e -> {
            currentUser = null;
            loginEmailField.setText("");
            loginPassField.setText("");
            cardLayout.show(mainPanel, "LOGIN");
        });

        // Center Tabs
        tabbedPane = new JTabbedPane();

        // Table Models
        slotsModel = new NonEditableTableModel(
                new String[] { "Slot ID", "Doctor Name", "Date", "Time", "Mode", "Status" }, 0);
        slotsTable = new JTable(slotsModel);

        appointmentsModel = new NonEditableTableModel(
                new String[] { "Appointment ID", "Doctor Name", "Patient Name", "Status", "Reason" }, 0);
        appointmentsTable = new JTable(appointmentsModel);

        queueModel = new NonEditableTableModel(
                new String[] { "Queue Position", "Appointment ID", "Doctor Name", "Patient Name", "Status" }, 0);
        queueTable = new JTable(queueModel);

        // 1. Consultation Slots Tab
        JPanel slotsPanel = new JPanel(new BorderLayout());
        slotsPanel.add(new JScrollPane(slotsTable), BorderLayout.CENTER);

        JPanel slotButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bookBtn = createStyledButton("Book Selected Slot");
        addSlotBtn = createStyledButton("Add Schedule Slot");
        updateSlotBtn = createStyledButton("Update Selected Slot");
        deleteSlotBtn = createStyledButton("Delete Selected Slot");

        slotButtonPanel.add(bookBtn);
        slotButtonPanel.add(addSlotBtn);
        slotButtonPanel.add(updateSlotBtn);
        slotButtonPanel.add(deleteSlotBtn);
        slotsPanel.add(slotButtonPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Consultation Slots", slotsPanel);

        // Book Action
        bookBtn.addActionListener(e -> {
            int row = slotsTable.getSelectedRow();
            if (row != -1) {
                String slotId = (String) slotsModel.getValueAt(row, 0);

                List<ScheduleSlot> slots = appointmentService.getAvailableSlots();
                String doctorId = null;
                for (ScheduleSlot s : slots) {
                    if (s.getSlotId().equals(slotId)) {
                        doctorId = s.getDoctorId();
                        break;
                    }
                }

                if (doctorId == null) {
                    JOptionPane.showMessageDialog(this, "Slot information not found.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String reason = JOptionPane.showInputDialog(this, "Enter Reason for Visit:");
                if (reason != null && !reason.trim().isEmpty()) {
                    Appointment app = appointmentService.bookAppointment(currentUser.getUserId(), doctorId, slotId,
                            reason.trim());
                    if (app != null) {
                        JOptionPane.showMessageDialog(this,
                                "Appointment Booked Successfully! ID: " + app.getAppointmentId());
                        refreshDashboardData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to book slot. Slot may no longer be AVAILABLE.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a slot from the table.");
            }
        });

        // Add Slot Action
        addSlotBtn.addActionListener(e -> {
            boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRoleLabel()) 
                           || "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel());

            JTextField doctorNameField = new JTextField(15);

            String[] years = {"2026", "2027"};
            String[] months = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
            String[] days = new String[31];
            for (int i = 1; i <= 31; i++) {
                days[i - 1] = String.format("%02d", i);
            }

            JComboBox<String> yearBox = new JComboBox<>(years);
            JComboBox<String> monthBox = new JComboBox<>(months);
            monthBox.setSelectedItem("09");
            JComboBox<String> dayBox = new JComboBox<>(days);
            dayBox.setSelectedItem("10");

            JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            datePanel.add(yearBox);
            datePanel.add(new JLabel("-"));
            datePanel.add(monthBox);
            datePanel.add(new JLabel("-"));
            datePanel.add(dayBox);

            String[] timeSlots = {
                "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", 
                "11:00", "11:30", "12:00", "14:00", "14:30", "15:00", 
                "15:30", "16:00", "16:30", "17:00"
            };

            JComboBox<String> startBox = new JComboBox<>(timeSlots);
            startBox.setSelectedItem("09:00");
            JComboBox<String> endBox = new JComboBox<>(timeSlots);
            endBox.setSelectedItem("10:00");

            JComboBox<String> modeBox = new JComboBox<>(new String[]{"IN_PERSON", "TELECONSULTATION"});

            JPanel inputPanel = new JPanel(new GridLayout(isAdmin ? 5 : 4, 2, 5, 5));

            if (isAdmin) {
                inputPanel.add(new JLabel("Doctor Name:"));
                inputPanel.add(doctorNameField);
            }

            inputPanel.add(new JLabel("Select Date (YYYY-MM-DD):"));
            inputPanel.add(datePanel);
            inputPanel.add(new JLabel("Start Time:"));
            inputPanel.add(startBox);
            inputPanel.add(new JLabel("End Time:"));
            inputPanel.add(endBox);
            inputPanel.add(new JLabel("Mode:"));
            inputPanel.add(modeBox);

            int result = JOptionPane.showConfirmDialog(this, inputPanel, "Create Schedule Slot", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String selectedDate = yearBox.getSelectedItem() + "-" + monthBox.getSelectedItem() + "-" + dayBox.getSelectedItem();
                String selectedStartTime = (String) startBox.getSelectedItem();
                String selectedEndTime = (String) endBox.getSelectedItem();

                String assignedDoctorName;
                if (isAdmin) {
                    assignedDoctorName = doctorNameField.getText().trim();
                    if (assignedDoctorName.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Doctor Name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } else {
                    assignedDoctorName = currentUser.getFullName();
                }

                ScheduleSlot slot = operationService.addScheduleSlot(
                        currentUser,
                        assignedDoctorName,
                        selectedDate,
                        selectedStartTime,
                        selectedEndTime,
                        (String) modeBox.getSelectedItem()
                );

                if (slot != null) {
                    JOptionPane.showMessageDialog(this, "Slot created successfully! Assigned ID: " + slot.getSlotId());
                    refreshDashboardData();
                } else {
                    JOptionPane.showMessageDialog(this, "Operation failed. Authorized DOCTOR or ADMIN role required.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

     // Update Slot Action
        updateSlotBtn.addActionListener(e -> {
            int row = slotsTable.getSelectedRow();
            if (row != -1) {
                String slotId = (String) slotsModel.getValueAt(row, 0);
                String currentDoctor = (String) slotsModel.getValueAt(row, 1);
                String currentDate = (String) slotsModel.getValueAt(row, 2);
                String currentMode = (String) slotsModel.getValueAt(row, 4);
                String currentStatus = (String) slotsModel.getValueAt(row, 5);

                boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRoleLabel()) 
                               || "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel());

                JTextField doctorNameField = new JTextField(currentDoctor, 10);
                JTextField dateField = new JTextField(currentDate, 10);
                JTextField startField = new JTextField("09:00", 10);
                JTextField endField = new JTextField("10:00", 10);
                
                JComboBox<String> modeBox = new JComboBox<>(new String[] { "IN_PERSON", "TELECONSULTATION" });
                modeBox.setSelectedItem(currentMode);
                
                JComboBox<String> statusBox = new JComboBox<>(new String[] { "AVAILABLE", "BLOCKED", "SCHEDULED" });
                statusBox.setSelectedItem(currentStatus);

                // Adjust row count based on role (6 rows for Admin, 5 rows for Doctor)
                JPanel inputPanel = new JPanel(new GridLayout(isAdmin ? 6 : 5, 2, 5, 5));

                if (isAdmin) {
                    inputPanel.add(new JLabel("Doctor Name:"));
                    inputPanel.add(doctorNameField);
                }

                inputPanel.add(new JLabel("New Date:"));
                inputPanel.add(dateField);
                inputPanel.add(new JLabel("Start Time:"));
                inputPanel.add(startField);
                inputPanel.add(new JLabel("End Time:"));
                inputPanel.add(endField);
                inputPanel.add(new JLabel("Mode:"));
                inputPanel.add(modeBox);
                inputPanel.add(new JLabel("Status:"));
                inputPanel.add(statusBox);

                int result = JOptionPane.showConfirmDialog(this, inputPanel, "Update Slot " + slotId,
                        JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    String updatedDoctorName = isAdmin ? doctorNameField.getText().trim() : currentUser.getFullName();
                    
                    if (isAdmin && updatedDoctorName.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Doctor Name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    boolean updated = operationService.updateScheduleSlot(
                            currentUser, 
                            slotId,
                            updatedDoctorName, // Pass doctor name here
                            dateField.getText().trim(), 
                            startField.getText().trim(), 
                            endField.getText().trim(),
                            (String) modeBox.getSelectedItem(), 
                            (String) statusBox.getSelectedItem()
                    );

                    if (updated) {
                        JOptionPane.showMessageDialog(this, "Slot updated successfully.");
                        refreshDashboardData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Update failed. Unauthorized or invalid slot.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a slot to update.");
            }
        });
        
        // Delete Slot Action
        deleteSlotBtn.addActionListener(e -> {
            int row = slotsTable.getSelectedRow();
            if (row != -1) {
                String slotId = (String) slotsModel.getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete slot " + slotId + "?", "Confirm Deletion",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean deleted = operationService.deleteScheduleSlot(currentUser, slotId);
                    if (deleted) {
                        JOptionPane.showMessageDialog(this, "Slot deleted successfully.");
                        refreshDashboardData();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Deletion rejected. Slot contains active appointments or access is denied.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a slot to delete.");
            }
        });

        // 2. Appointments & Status Tracking Tab
        JPanel appPanel = new JPanel(new BorderLayout());
        appPanel.add(new JScrollPane(appointmentsTable), BorderLayout.CENTER);

        JPanel appButtonPanel = new JPanel(new FlowLayout());
        JButton checkInBtn = createStyledButton("Check-In Patient");
        JButton updateStatusBtn = createStyledButton("Update Status");
        JButton cancelBtn = createStyledButton("Cancel Selected Appointment");

        appButtonPanel.add(checkInBtn);
        appButtonPanel.add(updateStatusBtn);
        appButtonPanel.add(cancelBtn);
        appPanel.add(appButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Appointments & Tracking", appPanel);

        checkInBtn.addActionListener(e -> {
            int row = appointmentsTable.getSelectedRow();
            if (row != -1) {
                String appId = (String) appointmentsModel.getValueAt(row, 0);
                if (operationService.checkInPatient(currentUser, appId)) {
                    JOptionPane.showMessageDialog(this, "Patient Checked In. Status updated to WAITING.");
                    refreshDashboardData();
                } else {
                    JOptionPane.showMessageDialog(this, "Check-in failed. Doctor/Admin role required.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an appointment.");
            }
        });

        updateStatusBtn.addActionListener(e -> {
            int row = appointmentsTable.getSelectedRow();
            if (row != -1) {
                String appId = (String) appointmentsModel.getValueAt(row, 0);
                String[] statusOptions = { "SCHEDULED", "WAITING", "IN_CONSULTATION", "COMPLETED", "CANCELLED" };
                String selectedStatus = (String) JOptionPane.showInputDialog(this, "Select new status:",
                        "Update Appointment Status", JOptionPane.QUESTION_MESSAGE, null, statusOptions,
                        statusOptions[0]);

                if (selectedStatus != null) {
                    statusService.updateAppointmentStatus(appId, selectedStatus);
                    JOptionPane.showMessageDialog(this, "Status updated to: " + selectedStatus);
                    refreshDashboardData();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an appointment.");
            }
        });

        cancelBtn.addActionListener(e -> {
            int row = appointmentsTable.getSelectedRow();
            if (row != -1) {
                String appId = (String) appointmentsModel.getValueAt(row, 0);
                String reason = JOptionPane.showInputDialog(this, "Cancellation Reason:");
                if (reason != null && !reason.trim().isEmpty()) {
                    if (appointmentService.cancelAppointment(appId, reason.trim())) {
                        JOptionPane.showMessageDialog(this, "Appointment Cancelled.");
                        refreshDashboardData();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an appointment.");
            }
        });

        // 3. Queue Management Panel Template with Admin View Filter
        queuePanel = new JPanel(new BorderLayout(5, 5));

        queueHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        queueDoctorFilterBox = new JComboBox<>();
        queueHeaderPanel.add(new JLabel("Filter Queue by Doctor:"));
        queueHeaderPanel.add(queueDoctorFilterBox);

        queueDoctorFilterBox.addActionListener(e -> {
            if (currentUser != null && ("ADMIN".equalsIgnoreCase(currentUser.getRoleLabel()) || "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel()))) {
                refreshQueueTable();
            }
        });

        queuePanel.add(queueHeaderPanel, BorderLayout.NORTH);
        queuePanel.add(new JScrollPane(queueTable), BorderLayout.CENTER);

        JButton callNextBtn = createStyledButton("Call Next Patient into Consultation");
        callNextBtn.setFont(new Font("Arial", Font.BOLD, 13));
        queuePanel.add(callNextBtn, BorderLayout.SOUTH);

        callNextBtn.addActionListener(e -> {
            if (currentUser == null) return;

            String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
            boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");

            String targetDoctor = currentUser.getFullName();
            if (isAdmin && queueDoctorFilterBox.getSelectedItem() != null) {
                String selectedFilter = (String) queueDoctorFilterBox.getSelectedItem();
                targetDoctor = "ALL DOCTORS".equalsIgnoreCase(selectedFilter) ? "" : selectedFilter;
            }

            Appointment nextApp = operationService.callNextPatient(currentUser, targetDoctor);
            if (nextApp != null) {
                JOptionPane.showMessageDialog(this,
                        "Calling Patient: " + nextApp.getPatientId() + "\nAppointment ID: "
                                + nextApp.getAppointmentId() + "\nStatus set to IN_CONSULTATION.\nReminder Sent!");
                refreshDashboardData();
            } else {
                JOptionPane.showMessageDialog(this, "No patients currently waiting in queue or access denied.");
            }
        });

        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    // ==================== USER REGISTRATION & PROFILE ====================

    private void showRegisterDialog() {
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleBox = new JComboBox<>(new String[] { "PATIENT", "DOCTOR" });

        JTextField dobField = new JTextField("2000-01-01");
        JTextField genderField = new JTextField("Not Specified");
        JTextField addressField = new JTextField("Not Provided");
        JTextField specializationField = new JTextField("General Practice");
        JTextField departmentField = new JTextField("Outpatient");
        JTextField feeField = new JTextField("80.00");

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Role:"));
        panel.add(roleBox);

        roleBox.addActionListener(e -> {
            boolean doctor = "DOCTOR".equals(roleBox.getSelectedItem());
            specializationField.setEnabled(doctor);
            departmentField.setEnabled(doctor);
            feeField.setEnabled(doctor);
            dobField.setEnabled(!doctor);
            genderField.setEnabled(!doctor);
            addressField.setEnabled(!doctor);
        });

        // Patient-specific fields
        panel.add(new JLabel("Date of Birth:"));
        panel.add(dobField);
        panel.add(new JLabel("Gender:"));
        panel.add(genderField);
        panel.add(new JLabel("Address:"));
        panel.add(addressField);

        // Doctor-specific fields
        panel.add(new JLabel("Specialization:"));
        panel.add(specializationField);
        panel.add(new JLabel("Department:"));
        panel.add(departmentField);
        panel.add(new JLabel("Consultation Fee:"));
        panel.add(feeField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Register Account", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in Full Name, Email, Phone Number and Password.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectedRole = (String) roleBox.getSelectedItem();

        if ("PATIENT".equals(selectedRole)) {
            Patient patient = userService.registerPatient(
                    name, email, phone, password,
                    dobField.getText().trim(),
                    genderField.getText().trim(),
                    addressField.getText().trim());

            if (patient != null) {
                JOptionPane.showMessageDialog(this,
                        "Patient account created successfully!\nUser ID: " + patient.getUserId());
            } else {
                JOptionPane.showMessageDialog(this,
                        "Registration failed. Email may already be registered.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            double fee;
            try {
                fee = Double.parseDouble(feeField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Consultation fee must be a valid number.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Doctor doctor = userService.registerDoctor(
                    name, email, phone, password,
                    specializationField.getText().trim(),
                    departmentField.getText().trim(), fee);

            if (doctor != null) {
                JOptionPane.showMessageDialog(this,
                        "Doctor account created successfully!\nUser ID: " + doctor.getUserId());
            } else {
                JOptionPane.showMessageDialog(this,
                        "Registration failed. Email may already be registered.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showProfileDialog() {
        if (currentUser == null) return;

        JTextField nameField = new JTextField(currentUser.getFullName());
        JTextField emailField = new JTextField(currentUser.getEmail());
        JTextField phoneField = new JTextField(currentUser.getPhoneNumber());

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneField);
        panel.add(new JLabel("Role:"));
        panel.add(new JLabel(currentUser.getRoleLabel()));
        panel.add(new JLabel("Account Status:"));
        panel.add(new JLabel(currentUser.getAccountStatus()));

        if (currentUser instanceof Patient) {
            Patient patient = (Patient) currentUser;
            panel.add(new JLabel("Patient No:"));
            panel.add(new JLabel(patient.getPatientNo()));
            panel.add(new JLabel("Date of Birth:"));
            panel.add(new JLabel(patient.getDateOfBirth()));
            panel.add(new JLabel("Gender:"));
            panel.add(new JLabel(patient.getGender()));
            panel.add(new JLabel("Address:"));
            panel.add(new JLabel(patient.getAddress()));
        } else if (currentUser instanceof Doctor) {
            Doctor doctor = (Doctor) currentUser;
            panel.add(new JLabel("Doctor ID:"));
            panel.add(new JLabel(doctor.getDoctorId()));
            panel.add(new JLabel("Specialization:"));
            panel.add(new JLabel(doctor.getSpecialization()));
            panel.add(new JLabel("Department:"));
            panel.add(new JLabel(doctor.getDepartment()));
        } else if (currentUser instanceof Administrator) {
            Administrator admin = (Administrator) currentUser;
            panel.add(new JLabel("Admin ID:"));
            panel.add(new JLabel(admin.getAdminId()));
            panel.add(new JLabel("Staff Department:"));
            panel.add(new JLabel(admin.getStaffDepartment()));
        }

        int result = JOptionPane.showConfirmDialog(
                this, panel, "My Profile", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            boolean updated = userService.updateProfile(
                    currentUser,
                    currentUser.getEmail(),
                    emailField.getText().trim(),
                    nameField.getText().trim(),
                    phoneField.getText().trim());

            if (updated) {
                JOptionPane.showMessageDialog(this, "Profile updated successfully!");
                userInfoLabel.setText("Logged in as: " + currentUser.getFullName()
                        + " (" + currentUser.getRoleLabel() + ")");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Profile update failed. Check the email or authorization.",
                        "Update Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        showProfileActionsDialog();
    }

    private void showProfileActionsDialog() {
        if (currentUser == null) return;

        JButton changePasswordButton = createStyledButton("Change Password");
        JButton deactivateButton = createStyledButton("Deactivate Account");
        JButton closeButton = createStyledButton("Close");

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(changePasswordButton);
        panel.add(deactivateButton);
        panel.add(closeButton);

        JDialog dialog = new JDialog(this, "Account Actions", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JLabel("Manage your account:", SwingConstants.CENTER), BorderLayout.NORTH);
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(420, 130);
        dialog.setLocationRelativeTo(this);

        changePasswordButton.addActionListener(e -> showChangePasswordDialog());

        deactivateButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Are you sure you want to deactivate your account?",
                    "Confirm Deactivation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = userService.deactivateAccount(
                        currentUser, currentUser.getEmail());

                if (success) {
                    JOptionPane.showMessageDialog(dialog, "Account deactivated successfully.");
                    dialog.dispose();
                    currentUser = null;
                    loginEmailField.setText("");
                    loginPassField.setText("");
                    cardLayout.show(mainPanel, "LOGIN");
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Unable to deactivate account.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showChangePasswordDialog() {
        if (currentUser == null) return;

        JPasswordField oldPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmPassword = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Current Password:"));
        panel.add(oldPassword);
        panel.add(new JLabel("New Password:"));
        panel.add(newPassword);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPassword);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String oldPass = new String(oldPassword.getPassword());
        String newPass = new String(newPassword.getPassword());
        String confirmPass = new String(confirmPassword.getPassword());

        if (newPass.isEmpty() || !newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this,
                    "New passwords do not match or are empty.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (userService.changePassword(currentUser, oldPass, newPass)) {
            JOptionPane.showMessageDialog(this, "Password changed successfully!");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Current password is incorrect.",
                    "Password Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showManageUsersDialog() {
        if (currentUser == null || !userService.hasPermission(currentUser, "PERM_MANAGE_USER")) {
            JOptionPane.showMessageDialog(this,
                    "Access denied. Administrator permission required.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<User> users = userService.getAllUsers();
        String[] columns = { "Name", "Email", "Role", "Phone", "Status" };
        DefaultTableModel model = new NonEditableTableModel(columns, 0);

        for (User user : users) {
            model.addRow(new Object[] {
                    user.getFullName(), user.getEmail(), user.getRoleLabel(),
                    user.getPhoneNumber(), user.getAccountStatus()
            });
        }

        JTable table = new JTable(model);
        JButton editButton = createStyledButton("Edit Selected User");
        JButton deactivateButton = createStyledButton("Deactivate Selected User");
        JButton closeButton = createStyledButton("Close");

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(editButton);
        buttons.add(deactivateButton);
        buttons.add(closeButton);

        JDialog dialog = new JDialog(this, "Manage Users", true);
        dialog.setLayout(new BorderLayout(5, 5));
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(750, 400);
        dialog.setLocationRelativeTo(this);

        editButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select a user.");
                return;
            }

            String oldEmail = (String) model.getValueAt(row, 1);
            User target = userService.getUserByEmail(oldEmail);
            if (target == null) return;

            JTextField nameField = new JTextField(target.getFullName());
            JTextField emailField = new JTextField(target.getEmail());
            JTextField phoneField = new JTextField(target.getPhoneNumber());

            JPanel editPanel = new JPanel(new GridLayout(0, 2, 5, 5));
            editPanel.add(new JLabel("Full Name:"));
            editPanel.add(nameField);
            editPanel.add(new JLabel("Email:"));
            editPanel.add(emailField);
            editPanel.add(new JLabel("Phone Number:"));
            editPanel.add(phoneField);

            int result = JOptionPane.showConfirmDialog(
                    dialog, editPanel, "Update User Profile", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                if (userService.updateProfile(currentUser, oldEmail,
                        emailField.getText().trim(), nameField.getText().trim(),
                        phoneField.getText().trim())) {
                    model.setValueAt(nameField.getText().trim(), row, 0);
                    model.setValueAt(emailField.getText().trim(), row, 1);
                    model.setValueAt(phoneField.getText().trim(), row, 3);
                    JOptionPane.showMessageDialog(dialog, "User profile updated.");
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Update failed. The email may already exist.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        deactivateButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select a user.");
                return;
            }

            String email = (String) model.getValueAt(row, 1);
            if (email.equalsIgnoreCase(currentUser.getEmail())) {
                JOptionPane.showMessageDialog(dialog,
                        "Use My Profile to deactivate your own account.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    dialog, "Deactivate account " + email + "?",
                    "Confirm Deactivation", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION
                    && userService.deactivateAccount(currentUser, email)) {
                model.setValueAt("INACTIVE", row, 4);
                JOptionPane.showMessageDialog(dialog, "User account deactivated.");
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void applyRolePermissions() {
        if (currentUser == null)
            return;

        String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
        userInfoLabel.setText("Logged in as: " + currentUser.getFullName() + " (" + role + ")");

        boolean canBook = userService.hasPermission(currentUser, "PERM_BOOK_APPOINTMENT");
        boolean canManageSlots = userService.hasPermission(currentUser, "PERM_MANAGE_SLOTS");
        boolean canManageUsers = userService.hasPermission(currentUser, "PERM_MANAGE_USER");
        boolean isAdmin = canManageUsers;
        boolean isStaff = canManageSlots || role.equals("DOCTOR") || isAdmin;

        // UI visibility follows RBAC permissions.
        bookBtn.setVisible(canBook);
        addSlotBtn.setVisible(canManageSlots);
        updateSlotBtn.setVisible(canManageSlots);
        deleteSlotBtn.setVisible(canManageSlots);

        // Show Admin Queue Dropdown Filter only if Administrator
        queueHeaderPanel.setVisible(isAdmin);
        manageUsersBtn.setVisible(canManageUsers);

        if (isAdmin) {
            populateAdminDoctorFilter();
        }

        // Dynamically append/remove Queue Management tab based on authorized roles
        int queueTabIndex = -1;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if ("Consultation Queue".equals(tabbedPane.getTitleAt(i))) {
                queueTabIndex = i;
                break;
            }
        }

        if (isStaff && queueTabIndex == -1) {
            tabbedPane.addTab("Consultation Queue", queuePanel);
        } else if (!isStaff && queueTabIndex != -1) {
            tabbedPane.removeTabAt(queueTabIndex);
        }
    }

    private void populateAdminDoctorFilter() {
        queueDoctorFilterBox.removeAllItems();
        queueDoctorFilterBox.addItem("ALL DOCTORS");

        List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(currentUser);
        if (allSlots != null) {
            for (ScheduleSlot s : allSlots) {
                String docName = s.getDoctorId();
                if (docName != null && !docName.trim().isEmpty()) {
                    boolean exists = false;
                    for (int i = 0; i < queueDoctorFilterBox.getItemCount(); i++) {
                        if (docName.equalsIgnoreCase(queueDoctorFilterBox.getItemAt(i))) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        queueDoctorFilterBox.addItem(docName);
                    }
                }
            }
        }
    }

    private void refreshDashboardData() {
        if (currentUser == null)
            return;

        String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
        boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");
        boolean isDoctor = role.equals("DOCTOR");

        // Refresh Slots Table
        slotsModel.setRowCount(0);

        if (isAdmin) {
            List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(currentUser);
            if (allSlots != null) {
                for (ScheduleSlot s : allSlots) {
                    slotsModel.addRow(new Object[] { 
                        s.getSlotId(), s.getDoctorId(), s.getSlotDate(),
                        s.getStartTime() + " - " + s.getEndTime(), s.getMode(), s.getStatus() 
                    });
                }
            }
        } else if (isDoctor) {
            List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(currentUser);
            if (allSlots != null) {
                for (ScheduleSlot s : allSlots) {
                    if (currentUser.getFullName().equalsIgnoreCase(s.getDoctorId())) {
                        slotsModel.addRow(new Object[] { 
                            s.getSlotId(), s.getDoctorId(), s.getSlotDate(),
                            s.getStartTime() + " - " + s.getEndTime(), s.getMode(), s.getStatus() 
                        });
                    }
                }
            }
        } else {
            List<ScheduleSlot> availableSlots = appointmentService.getAvailableSlots();
            if (availableSlots != null) {
                for (ScheduleSlot s : availableSlots) {
                    if ("SCHEDULED".equalsIgnoreCase(s.getStatus()) || "AVAILABLE".equalsIgnoreCase(s.getStatus())) {
                        slotsModel.addRow(new Object[] { 
                            s.getSlotId(), s.getDoctorId(), s.getSlotDate(),
                            s.getStartTime() + " - " + s.getEndTime(), s.getMode(), s.getStatus() 
                        });
                    }
                }
            }
        }

        // Refresh Appointments Table
        appointmentsModel.setRowCount(0);
        List<Appointment> apps;
        if (isDoctor) {
            apps = appointmentService.getAppointmentsByDoctor(currentUser.getUserId());
        } else if (isAdmin) {
            apps = appointmentService.getAllAppointments();
        } else {
            apps = appointmentService.getAppointmentsByPatient(currentUser.getUserId());
        }

        if (apps != null) {
            for (Appointment a : apps) {
                appointmentsModel.addRow(new Object[] { 
                    a.getAppointmentId(), a.getDoctorId(), a.getPatientId(),
                    a.getStatus(), a.getReasonForVisit() 
                });
            }
        }

        // Refresh Queue Table (Doctors & Admins)
        if (isDoctor || isAdmin) {
            refreshQueueTable();
        }
    }

    private void refreshQueueTable() {
        queueModel.setRowCount(0);
        String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
        boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");

        String targetDoctor = currentUser.getFullName();
        if (isAdmin && queueDoctorFilterBox.getSelectedItem() != null) {
            String selectedFilter = (String) queueDoctorFilterBox.getSelectedItem();
            targetDoctor = "ALL DOCTORS".equalsIgnoreCase(selectedFilter) ? "" : selectedFilter;
        }

        Queue<Appointment> queue = operationService.getDoctorQueue(currentUser, targetDoctor);
        if (queue != null) {
            int queuePosition = 1;
            for (Appointment a : queue) {
                queueModel.addRow(new Object[] { 
                    queuePosition++, a.getAppointmentId(), a.getDoctorId(), a.getPatientId(), a.getStatus() 
                });
            }
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        return button;
    }

    public void start() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private static class NonEditableTableModel extends DefaultTableModel {
        public NonEditableTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}