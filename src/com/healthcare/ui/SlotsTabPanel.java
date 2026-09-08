package com.healthcare.ui;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.HealthcareOperationService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SlotsTabPanel extends JPanel {
    private final HealthcareUI mainFrame;
    private final DashboardPanel dashboard;

    private final AppointmentService appointmentService = new AppointmentService();
    private final HealthcareOperationService operationService = new HealthcareOperationService();

    private final DefaultTableModel slotsModel = new HealthcareUI.NonEditableTableModel(
            new String[] { "Slot ID", "Doctor Name", "Date", "Time", "Mode", "Status" }, 0);
    private final JTable slotsTable = new JTable(slotsModel);

    private JButton bookBtn;
    private JButton addSlotBtn;
    private JButton updateSlotBtn;
    private JButton deleteSlotBtn;

    public SlotsTabPanel(HealthcareUI mainFrame, DashboardPanel dashboard) {
        this.mainFrame = mainFrame;
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        add(new JScrollPane(slotsTable), BorderLayout.CENTER);

        JPanel slotButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bookBtn = HealthcareUI.createStyledButton("Book Selected Slot");
        addSlotBtn = HealthcareUI.createStyledButton("Add Schedule Slot");
        updateSlotBtn = HealthcareUI.createStyledButton("Update Selected Slot");
        deleteSlotBtn = HealthcareUI.createStyledButton("Delete Selected Slot");

        slotButtonPanel.add(bookBtn);
        slotButtonPanel.add(addSlotBtn);
        slotButtonPanel.add(updateSlotBtn);
        slotButtonPanel.add(deleteSlotBtn);
        add(slotButtonPanel, BorderLayout.SOUTH);

        bookBtn.addActionListener(e -> handleBookSlot());
        addSlotBtn.addActionListener(e -> handleAddSlot());
        updateSlotBtn.addActionListener(e -> handleUpdateSlot());
        deleteSlotBtn.addActionListener(e -> handleDeleteSlot());
    }

    public void applyRolePermissions(User currentUser, boolean isStaff) {
        bookBtn.setVisible(!isStaff);
        addSlotBtn.setVisible(isStaff);
        updateSlotBtn.setVisible(isStaff);
        deleteSlotBtn.setVisible(isStaff);
    }

    private boolean isDoctorSlotMatch(User doctor, String slotDoctorId) {
        if (doctor == null || slotDoctorId == null) return false;

        String docId = doctor.getUserId() != null ? doctor.getUserId().trim() : "";
        String docName = doctor.getFullName() != null ? doctor.getFullName().trim() : "";
        String target = slotDoctorId.trim();

        if (target.equalsIgnoreCase(docId) || target.equalsIgnoreCase(docName)) {
            return true;
        }

        String cleanTarget = target.replaceAll("(?i)^dr\\.\\s*", "").trim();
        String cleanDocName = docName.replaceAll("(?i)^dr\\.\\s*", "").trim();

        return cleanTarget.equalsIgnoreCase(cleanDocName);
    }

    public void refreshData(User currentUser) {
        slotsModel.setRowCount(0);
        String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
        boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");
        boolean isDoctor = role.equals("DOCTOR");

        List<ScheduleSlot> rawSlots = new ArrayList<>();

        if (isAdmin) {
            List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(currentUser);
            if (allSlots != null) rawSlots.addAll(allSlots);
        } else if (isDoctor) {
            List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(currentUser);
            if (allSlots != null) {
                for (ScheduleSlot s : allSlots) {
                    if (isDoctorSlotMatch(currentUser, s.getDoctorId())) {
                        rawSlots.add(s);
                    }
                }
            }
        } else {
            List<ScheduleSlot> availableSlots = appointmentService.getAvailableSlots();
            if (availableSlots != null) rawSlots.addAll(availableSlots);
        }

        rawSlots.sort(Comparator.comparing(ScheduleSlot::getSlotDate)
                .thenComparing(ScheduleSlot::getStartTime));

        for (ScheduleSlot s : rawSlots) {
            slotsModel.addRow(new Object[] { s.getSlotId(), s.getDoctorId(), s.getSlotDate(),
                    s.getStartTime() + " - " + s.getEndTime(), s.getMode(), s.getStatus() });
        }
    }

    private boolean hasTimeClash(String doctorName, String date, String startTime, String endTime, String excludeSlotId) {
        List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(mainFrame.getCurrentUser());
        if (allSlots == null) return false;

        for (ScheduleSlot slot : allSlots) {
            if (excludeSlotId != null && slot.getSlotId().equalsIgnoreCase(excludeSlotId)) {
                continue;
            }

            if (isDoctorSlotMatch(mainFrame.getCurrentUser(), slot.getDoctorId()) && slot.getSlotDate().equals(date)) {
                if (startTime.compareTo(slot.getEndTime()) < 0 && endTime.compareTo(slot.getStartTime()) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleBookSlot() {
        int row = slotsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a slot from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String slotId = (String) slotsModel.getValueAt(row, 0);
        String doctorId = (String) slotsModel.getValueAt(row, 1);
        String currentStatus = (String) slotsModel.getValueAt(row, 5);

        if (!"AVAILABLE".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(mainFrame, "This slot is no longer AVAILABLE for booking.", "Slot Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField reasonField = new JTextField(20);
        JPanel panelDialog = new JPanel(new GridLayout(2, 1, 5, 5));
        panelDialog.add(new JLabel("Enter Reason for Visit:"));
        panelDialog.add(reasonField);

        int option = JOptionPane.showConfirmDialog(mainFrame, panelDialog, "Book Appointment - Slot: " + slotId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String reason = reasonField.getText().trim();
            if (!reason.isEmpty()) {
                Appointment app = appointmentService.bookAppointment(mainFrame.getCurrentUser().getUserId(), doctorId, slotId, reason);
                if (app != null) {
                    JOptionPane.showMessageDialog(mainFrame, "Appointment Booked Successfully! ID: " + app.getAppointmentId());
                    refreshData(mainFrame.getCurrentUser());
                    dashboard.refreshDashboardData();
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Failed to book slot. Slot may no longer be AVAILABLE.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Reason for visit is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleAddSlot() {
        User currentUser = mainFrame.getCurrentUser();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRoleLabel()) || "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel());

        JTextField doctorNameField = new JTextField(15);
        String[] years = { "2026", "2027" };
        String[] months = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };
        String[] days = new String[31];
        for (int i = 1; i <= 31; i++) days[i - 1] = String.format("%02d", i);

        JComboBox<String> yearBox = new JComboBox<>(years);
        JComboBox<String> monthBox = new JComboBox<>(months); monthBox.setSelectedItem("09");
        JComboBox<String> dayBox = new JComboBox<>(days); dayBox.setSelectedItem("10");

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        datePanel.add(yearBox); datePanel.add(new JLabel("-"));
        datePanel.add(monthBox); datePanel.add(new JLabel("-"));
        datePanel.add(dayBox);

        String[] timeSlots = { "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00" };
        JComboBox<String> startBox = new JComboBox<>(timeSlots); startBox.setSelectedItem("09:00");
        JComboBox<String> endBox = new JComboBox<>(timeSlots); endBox.setSelectedItem("10:00");

        JComboBox<String> modeBox = new JComboBox<>(new String[] { "IN_PERSON", "TELECONSULTATION" });
        
        JTextField statusField = new JTextField("AVAILABLE");
        statusField.setEditable(false);

        JPanel inputPanel = new JPanel(new GridLayout(isAdmin ? 6 : 5, 2, 5, 5));

        if (isAdmin) { inputPanel.add(new JLabel("Doctor Name:")); inputPanel.add(doctorNameField); }
        inputPanel.add(new JLabel("Select Date (YYYY-MM-DD):")); inputPanel.add(datePanel);
        inputPanel.add(new JLabel("Start Time:")); inputPanel.add(startBox);
        inputPanel.add(new JLabel("End Time:")); inputPanel.add(endBox);
        inputPanel.add(new JLabel("Mode:")); inputPanel.add(modeBox);
        inputPanel.add(new JLabel("Status:")); inputPanel.add(statusField);

        int result = JOptionPane.showConfirmDialog(mainFrame, inputPanel, "Create Schedule Slot", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String selectedDate = yearBox.getSelectedItem() + "-" + monthBox.getSelectedItem() + "-" + dayBox.getSelectedItem();
            String assignedDoctorName = isAdmin ? doctorNameField.getText().trim() : currentUser.getFullName();
            String startTime = (String) startBox.getSelectedItem();
            String endTime = (String) endBox.getSelectedItem();
            String defaultStatus = "AVAILABLE";

            if (isAdmin && assignedDoctorName.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Doctor Name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (startTime.compareTo(endTime) >= 0) {
                JOptionPane.showMessageDialog(mainFrame, "Start Time must be strictly earlier than End Time.", "Time Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (hasTimeClash(assignedDoctorName, selectedDate, startTime, endTime, null)) {
                JOptionPane.showMessageDialog(mainFrame, "Time clash detected! " + assignedDoctorName + " already has a slot overlapping this period.", "Schedule Clash", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ScheduleSlot slot = operationService.addScheduleSlot(currentUser, assignedDoctorName, selectedDate,
                    startTime, endTime, (String) modeBox.getSelectedItem());

            if (slot != null) {
                slot.setStatus(defaultStatus);
                operationService.updateScheduleSlot(currentUser, slot.getSlotId(), assignedDoctorName, selectedDate, startTime, endTime, (String) modeBox.getSelectedItem(), defaultStatus);

                JOptionPane.showMessageDialog(mainFrame, "Slot created successfully! Assigned ID: " + slot.getSlotId());
                
                // Immediately refresh table UI & dashboard
                refreshData(currentUser);
                dashboard.refreshDashboardData();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Operation failed. Authorized DOCTOR or ADMIN role required.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleUpdateSlot() {
        int row = slotsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a slot to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User currentUser = mainFrame.getCurrentUser();
        String slotId = (String) slotsModel.getValueAt(row, 0);
        String currentDoctor = (String) slotsModel.getValueAt(row, 1);
        String currentDate = (String) slotsModel.getValueAt(row, 2);
        String timeRange = (String) slotsModel.getValueAt(row, 3);
        String currentMode = (String) slotsModel.getValueAt(row, 4);
        String currentStatus = (String) slotsModel.getValueAt(row, 5);

        String[] times = timeRange.split(" - ");
        String initialStart = times.length > 0 ? times[0].trim() : "09:00";
        String initialEnd = times.length > 1 ? times[1].trim() : "10:00";

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRoleLabel()) || "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel());

        JTextField doctorNameField = new JTextField(currentDoctor, 10);
        JTextField dateField = new JTextField(currentDate, 10);
        JTextField startField = new JTextField(initialStart, 10);
        JTextField endField = new JTextField(initialEnd, 10);

        JComboBox<String> modeBox = new JComboBox<>(new String[] { "IN_PERSON", "TELECONSULTATION" });
        modeBox.setSelectedItem(currentMode);
        JComboBox<String> statusBox = new JComboBox<>(new String[] { "AVAILABLE", "BLOCKED", "SCHEDULED" });
        statusBox.setSelectedItem(currentStatus);

        JPanel inputPanel = new JPanel(new GridLayout(isAdmin ? 6 : 5, 2, 5, 5));
        if (isAdmin) { inputPanel.add(new JLabel("Doctor Name:")); inputPanel.add(doctorNameField); }
        inputPanel.add(new JLabel("New Date (YYYY-MM-DD):")); inputPanel.add(dateField);
        inputPanel.add(new JLabel("Start Time (HH:MM):")); inputPanel.add(startField);
        inputPanel.add(new JLabel("End Time (HH:MM):")); inputPanel.add(endField);
        inputPanel.add(new JLabel("Mode:")); inputPanel.add(modeBox);
        inputPanel.add(new JLabel("Status:")); inputPanel.add(statusBox);

        int result = JOptionPane.showConfirmDialog(mainFrame, inputPanel, "Update Slot " + slotId, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String updatedDoctorName = isAdmin ? doctorNameField.getText().trim() : currentUser.getFullName();
            String newDate = dateField.getText().trim();
            String startTime = startField.getText().trim();
            String endTime = endField.getText().trim();

            if (isAdmin && updatedDoctorName.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Doctor Name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (startTime.compareTo(endTime) >= 0) {
                JOptionPane.showMessageDialog(mainFrame, "Start Time must be strictly earlier than End Time.", "Time Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (hasTimeClash(updatedDoctorName, newDate, startTime, endTime, slotId)) {
                JOptionPane.showMessageDialog(mainFrame, "Time clash detected! " + updatedDoctorName + " already has a slot overlapping this period.", "Schedule Clash", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean updated = operationService.updateScheduleSlot(currentUser, slotId, updatedDoctorName,
                    newDate, startTime, endTime,
                    (String) modeBox.getSelectedItem(), (String) statusBox.getSelectedItem());

            if (updated) {
                JOptionPane.showMessageDialog(mainFrame, "Slot updated successfully.");
                refreshData(currentUser);
                dashboard.refreshDashboardData();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Update failed. Unauthorized or invalid slot.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteSlot() {
        int row = slotsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a slot to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String slotId = (String) slotsModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to delete slot " + slotId + "?",
                "Confirm Deletion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.OK_OPTION) {
            boolean deleted = operationService.deleteScheduleSlot(mainFrame.getCurrentUser(), slotId);
            if (deleted) {
                JOptionPane.showMessageDialog(mainFrame, "Slot deleted successfully.");
                refreshData(mainFrame.getCurrentUser());
                dashboard.refreshDashboardData();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Deletion rejected. Slot contains active appointments or access is denied.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}