package com.healthcare.ui;

import com.healthcare.model.Doctor;
import com.healthcare.model.Patient;
import com.healthcare.model.User;
import com.healthcare.service.ReportService;
import com.healthcare.service.UserService;
import com.healthcare.ui.HealthcareUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class UserManagementDialog {

    public static void showRegisterDialog(Component parent, UserService userService) {
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

        specializationField.setEnabled(false);
        departmentField.setEnabled(false);
        feeField.setEnabled(false);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Full Name:")); panel.add(nameField);
        panel.add(new JLabel("Email:")); panel.add(emailField);
        panel.add(new JLabel("Phone Number:")); panel.add(phoneField);
        panel.add(new JLabel("Password:")); panel.add(passwordField);
        panel.add(new JLabel("Role:")); panel.add(roleBox);
        panel.add(new JLabel("Date of Birth:")); panel.add(dobField);
        panel.add(new JLabel("Gender:")); panel.add(genderField);
        panel.add(new JLabel("Address:")); panel.add(addressField);
        panel.add(new JLabel("Specialization (Doctor):")); panel.add(specializationField);
        panel.add(new JLabel("Department (Doctor):")); panel.add(departmentField);
        panel.add(new JLabel("Consultation Fee ($):")); panel.add(feeField);

        roleBox.addActionListener(e -> {
            boolean doctor = "DOCTOR".equals(roleBox.getSelectedItem());
            specializationField.setEnabled(doctor);
            departmentField.setEnabled(doctor);
            feeField.setEnabled(doctor);
            dobField.setEnabled(!doctor);
            genderField.setEnabled(!doctor);
            addressField.setEnabled(!doctor);
        });

        int result = JOptionPane.showConfirmDialog(parent, panel, "Register New Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Name, Email, Phone Number, and Password are required fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User newUser;
        if ("DOCTOR".equals(role)) {
            double fee;
            try { fee = Double.parseDouble(feeField.getText().trim()); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Consultation fee must be a valid number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            newUser = userService.registerDoctor(name, email, phone, password, specializationField.getText().trim(), departmentField.getText().trim(), fee);
        } else {
            newUser = userService.registerPatient(name, email, phone, password, dobField.getText().trim(), genderField.getText().trim(), addressField.getText().trim());
        }

        if (newUser != null) {
            JOptionPane.showMessageDialog(parent, "Registration Successful!\nUser ID: " + newUser.getUserId());
        } else {
            JOptionPane.showMessageDialog(parent, "Registration failed. Email may already exist or input is invalid.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showProfileDialog(Component parent, UserService userService, User currentUser, Consumer<User> onUpdateCallback) {
        if (currentUser == null) return;

        JTextField nameField = new JTextField(currentUser.getFullName());
        JTextField emailField = new JTextField(currentUser.getEmail());
        JTextField phoneField = new JTextField(currentUser.getPhoneNumber());
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("User ID:")); panel.add(new JLabel(currentUser.getUserId()));
        panel.add(new JLabel("Full Name:")); panel.add(nameField);
        panel.add(new JLabel("Email:")); panel.add(emailField);
        panel.add(new JLabel("Phone Number:")); panel.add(phoneField);
        panel.add(new JLabel("Role:")); panel.add(new JLabel(currentUser.getRoleLabel()));
        panel.add(new JLabel("Status:")); panel.add(new JLabel(currentUser.getAccountStatus()));

        JTextField f1 = null, f2 = null, f3 = null;
        if (currentUser instanceof Patient) {
            Patient p = (Patient) currentUser;
            f1 = new JTextField(p.getDateOfBirth()); f2 = new JTextField(p.getGender()); f3 = new JTextField(p.getAddress());
            panel.add(new JLabel("Date of Birth:")); panel.add(f1);
            panel.add(new JLabel("Gender:")); panel.add(f2);
            panel.add(new JLabel("Address:")); panel.add(f3);
        } else if (currentUser instanceof Doctor) {
            Doctor d = (Doctor) currentUser;
            f1 = new JTextField(d.getSpecialization()); f2 = new JTextField(d.getDepartment()); f3 = new JTextField(String.valueOf(d.getConsultationFee()));
            panel.add(new JLabel("Specialization:")); panel.add(f1);
            panel.add(new JLabel("Department:")); panel.add(f2);
            panel.add(new JLabel("Consultation Fee:")); panel.add(f3);
        }

        int result = JOptionPane.showConfirmDialog(parent, panel, "My Profile - Update", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Double fee = null;
            if (currentUser instanceof Doctor) {
                try { fee = Double.parseDouble(f3.getText().trim()); if (fee < 0) throw new NumberFormatException(); }
                catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parent, "Consultation fee must be a valid non-negative number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            String oldEmail = currentUser.getEmail();
            boolean updated = userService.updateProfile(currentUser, oldEmail, emailField.getText().trim(),
                    nameField.getText().trim(), phoneField.getText().trim(),
                    currentUser instanceof Patient ? f1.getText().trim() : null,
                    currentUser instanceof Patient ? f2.getText().trim() : null,
                    currentUser instanceof Patient ? f3.getText().trim() : null,
                    currentUser instanceof Doctor ? f1.getText().trim() : null,
                    currentUser instanceof Doctor ? f2.getText().trim() : null, fee);

            if (updated) {
                User updatedUser = userService.getUserByEmail(emailField.getText().trim());
                if (onUpdateCallback != null) onUpdateCallback.accept(updatedUser);
                JOptionPane.showMessageDialog(parent, "Profile updated successfully!");
            } else {
                JOptionPane.showMessageDialog(parent, "Profile update failed. Please check your input.", "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void showManageUsersDialog(Component parent, UserService userService, User currentUser) {
        if (currentUser == null || !userService.hasPermission(currentUser, "PERM_MANAGE_USER")) {
            JOptionPane.showMessageDialog(parent, "Access denied. Administrator permission required.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<User> users = userService.getAllUsers();
        String[] columns = { "User ID", "Name", "Email", "Role", "Phone", "Status" };
        
        users.sort(Comparator.comparing(User::getUserId, (id1, id2) -> {
            int num1 = Integer.parseInt(id1.replaceAll("[^0-9]", ""));
            int num2 = Integer.parseInt(id2.replaceAll("[^0-9]", ""));
            return Integer.compare(num1, num2);
        }));
        
        DefaultTableModel model = new HealthcareUI.NonEditableTableModel(columns, 0);
        for (User u : users) {
            model.addRow(new Object[] { u.getUserId(), u.getFullName(), u.getEmail(), u.getRoleLabel(), u.getPhoneNumber(), u.getAccountStatus() });
        }

        JTable table = new JTable(model);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Manage Users", true);
        dialog.setLayout(new BorderLayout(5, 5));
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton updateButton = HealthcareUI.createStyledButton("Update Selected User");
        JButton deleteButton = HealthcareUI.createStyledButton("Delete Selected User");
        JButton resetButton = HealthcareUI.createStyledButton("Reset Password");
        JButton cancelButton = HealthcareUI.createStyledButton("Cancel");

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(updateButton); buttons.add(deleteButton); buttons.add(resetButton); buttons.add(cancelButton);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(900, 450);
        dialog.setLocationRelativeTo(parent);

        updateButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Please select a user."); return; }
            String oldEmail = (String) model.getValueAt(row, 2);
            User target = userService.getUserByEmail(oldEmail);
            if (target == null) return;

            JTextField name = new JTextField(target.getFullName());
            JTextField email = new JTextField(target.getEmail());
            JTextField phone = new JTextField(target.getPhoneNumber());
            JTextField f1 = null, f2 = null, f3 = null;
            JPanel edit = new JPanel(new GridLayout(0, 2, 5, 5));
            edit.add(new JLabel("Full Name:")); edit.add(name);
            edit.add(new JLabel("Email:")); edit.add(email);
            edit.add(new JLabel("Phone Number:")); edit.add(phone);

            if (target instanceof Patient) {
                Patient p = (Patient) target;
                f1 = new JTextField(p.getDateOfBirth()); f2 = new JTextField(p.getGender()); f3 = new JTextField(p.getAddress());
                edit.add(new JLabel("Date of Birth:")); edit.add(f1); edit.add(new JLabel("Gender:")); edit.add(f2); edit.add(new JLabel("Address:")); edit.add(f3);
            } else if (target instanceof Doctor) {
                Doctor d = (Doctor) target;
                f1 = new JTextField(d.getSpecialization()); f2 = new JTextField(d.getDepartment()); f3 = new JTextField(String.valueOf(d.getConsultationFee()));
                edit.add(new JLabel("Specialization:")); edit.add(f1); edit.add(new JLabel("Department:")); edit.add(f2); edit.add(new JLabel("Consultation Fee:")); edit.add(f3);
            }

            int result = JOptionPane.showConfirmDialog(dialog, edit, "Update User Profile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            Double fee = null;
            if (target instanceof Doctor) {
                try { fee = Double.parseDouble(f3.getText().trim()); if (fee < 0) throw new NumberFormatException(); }
                catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dialog, "Consultation fee must be a valid non-negative number.", "Validation Error", JOptionPane.WARNING_MESSAGE); return; }
            }

            boolean updated = userService.updateProfile(currentUser, oldEmail, email.getText().trim(), name.getText().trim(), phone.getText().trim(),
                    target instanceof Patient ? f1.getText().trim() : null,
                    target instanceof Patient ? f2.getText().trim() : null,
                    target instanceof Patient ? f3.getText().trim() : null,
                    target instanceof Doctor ? f1.getText().trim() : null,
                    target instanceof Doctor ? f2.getText().trim() : null, fee);

            if (updated) {
                model.setValueAt(name.getText().trim(), row, 1);
                model.setValueAt(email.getText().trim(), row, 2);
                model.setValueAt(phone.getText().trim(), row, 4);
                JOptionPane.showMessageDialog(dialog, "User profile updated successfully.");
            } else JOptionPane.showMessageDialog(dialog, "Update failed. Email may already exist or input is invalid.", "Update Error", JOptionPane.ERROR_MESSAGE);
        });

        deleteButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Please select a user."); return; }
            String email = (String) model.getValueAt(row, 2);
            if (email.equalsIgnoreCase(currentUser.getEmail())) { JOptionPane.showMessageDialog(dialog, "You cannot delete your own administrator account."); return; }
            int confirm = JOptionPane.showConfirmDialog(dialog, "Delete user account " + email + " permanently?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION && userService.deleteUser(currentUser, email)) {
                model.removeRow(row); JOptionPane.showMessageDialog(dialog, "User deleted successfully.");
            }
        });

        resetButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Please select a user."); return; }
            String email = (String) model.getValueAt(row, 2);
            JPasswordField newPass = new JPasswordField(); JPasswordField confirmPass = new JPasswordField();
            JPanel reset = new JPanel(new GridLayout(0, 2, 5, 5));
            reset.add(new JLabel("New Password:")); reset.add(newPass); reset.add(new JLabel("Confirm Password:")); reset.add(confirmPass);
            int result = JOptionPane.showConfirmDialog(dialog, reset, "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            String a = new String(newPass.getPassword()), b = new String(confirmPass.getPassword());
            if (a.isEmpty() || !a.equals(b)) { JOptionPane.showMessageDialog(dialog, "Passwords do not match or are empty.", "Validation Error", JOptionPane.WARNING_MESSAGE); return; }
            if (userService.resetPassword(currentUser, email, a)) JOptionPane.showMessageDialog(dialog, "Password reset successfully.");
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    public static void showGenerateReportDialog(Component parent) {
        JTextField startField = new JTextField(10);
        JTextField endField = new JTextField(10);
        JTextField categoryField = new JTextField(20);
        categoryField.setText("Appointment Status Report");

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.add(new JLabel("Start Date (YYYY-MM-DD):")); inputPanel.add(startField);
        inputPanel.add(new JLabel("End Date (YYYY-MM-DD):")); inputPanel.add(endField);
        inputPanel.add(new JLabel("Report Category:")); inputPanel.add(categoryField);

        int option = JOptionPane.showConfirmDialog(parent, inputPanel, "Generate Report", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startField.getText().trim());
                java.time.LocalDate end = java.time.LocalDate.parse(endField.getText().trim());
                String category = categoryField.getText().trim();

                ReportService reportService = new ReportService();
                String result = reportService.generateReport(start, end, category);

                JTextArea textArea = new JTextArea(result, 12, 40);
                textArea.setEditable(false);
                JOptionPane.showMessageDialog(parent, new JScrollPane(textArea), "Report Result", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Invalid date format. Please use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}