package com.healthcare.ui;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DatabaseStore;
import com.healthcare.service.HealthcareOperationService;
import com.healthcare.service.StatusTrackingService;
import com.healthcare.service.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AppointmentsTabPanel extends JPanel {
    private final HealthcareUI mainFrame;
    private final DashboardPanel dashboard;

    private final AppointmentService appointmentService = new AppointmentService();
    private final StatusTrackingService statusService = new StatusTrackingService();
    private final HealthcareOperationService operationService = new HealthcareOperationService();

    private final DefaultTableModel appointmentsModel = new HealthcareUI.NonEditableTableModel(
            new String[] { "Appointment ID", "Doctor Name", "Patient Name", "Date", "Time", "Status", "Reason" }, 0);
    private final JTable appointmentsTable = new JTable(appointmentsModel);

    private JButton checkInBtn;
    private JButton updateStatusBtn;
    private JButton openConsultBtn;
    private JButton rescheduleBtn;
    private JButton cancelBtn;

    public AppointmentsTabPanel(HealthcareUI mainFrame, DashboardPanel dashboard) {
        this.mainFrame = mainFrame;
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        add(new JScrollPane(appointmentsTable), BorderLayout.CENTER);

        JPanel appButtonPanel = new JPanel(new FlowLayout());
        checkInBtn = HealthcareUI.createStyledButton("Check-In Patient");
        updateStatusBtn = HealthcareUI.createStyledButton("Update Status");
        openConsultBtn = HealthcareUI.createStyledButton("Open Consultation");
        rescheduleBtn = HealthcareUI.createStyledButton("Reschedule Appointment");
        cancelBtn = HealthcareUI.createStyledButton("Cancel Selected Appointment");

        appButtonPanel.add(checkInBtn);
        appButtonPanel.add(updateStatusBtn);
        appButtonPanel.add(openConsultBtn);
        appButtonPanel.add(rescheduleBtn);
        appButtonPanel.add(cancelBtn);
        add(appButtonPanel, BorderLayout.SOUTH);

        checkInBtn.addActionListener(e -> handleCheckIn());
        updateStatusBtn.addActionListener(e -> handleUpdateStatus());
        openConsultBtn.addActionListener(e -> handleOpenConsultation());
        rescheduleBtn.addActionListener(e -> handleReschedule());
        cancelBtn.addActionListener(e -> handleCancelAppointment());
    }

    public void applyRolePermissions(boolean isStaff) {
        checkInBtn.setVisible(isStaff);
        updateStatusBtn.setVisible(isStaff);
        openConsultBtn.setVisible(isStaff);
        rescheduleBtn.setVisible(true);
        cancelBtn.setVisible(true);
    }

    public void refreshData(User currentUser) {
        appointmentsModel.setRowCount(0);
        if (currentUser == null) return;

        String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
        boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");
        boolean isDoctor = role.equals("DOCTOR");

        List<Appointment> apps = new ArrayList<>();

        if (isAdmin) {
            apps = statusService.getDoctorAppointments("");
            if (apps == null || apps.isEmpty()) {
                apps = new ArrayList<>(DatabaseStore.getInstance().getAppointments().values());
            }
        } else if (isDoctor) {
            apps = statusService.getDoctorAppointments(currentUser.getUserId());
            
            if (apps == null || apps.isEmpty()) {
                apps = statusService.getDoctorAppointments(currentUser.getFullName());
            }

            if (apps == null || apps.isEmpty()) {
                List<Appointment> allApps = new ArrayList<>(DatabaseStore.getInstance().getAppointments().values());
                for (Appointment a : allApps) {
                    String docId = a.getDoctorId();
                    String docName = resolveDoctorName(docId);
                    boolean matchesId = docId != null && docId.equalsIgnoreCase(currentUser.getUserId());
                    boolean matchesName = docName != null && docName.equalsIgnoreCase(currentUser.getFullName());
                    
                    if (matchesId || matchesName) {
                        apps.add(a);
                    }
                }
            }
        } else {
            apps = statusService.getPatientAppointments(currentUser.getUserId());
            if (apps == null || apps.isEmpty()) {
                List<Appointment> allApps = new ArrayList<>(DatabaseStore.getInstance().getAppointments().values());
                for (Appointment a : allApps) {
                    if (a.getPatientId() != null && a.getPatientId().equalsIgnoreCase(currentUser.getUserId())) {
                        apps.add(a);
                    }
                }
            }
        }

        if (apps != null) {
            // Sort list chronologically by Slot Date and Start Time
            apps.sort(Comparator.comparing((Appointment a) -> {
                ScheduleSlot slot = appointmentService.getSlotById(a.getSlotId());
                return (slot != null && slot.getSlotDate() != null) ? slot.getSlotDate() : "";
            }).thenComparing(a -> {
                ScheduleSlot slot = appointmentService.getSlotById(a.getSlotId());
                return (slot != null && slot.getStartTime() != null) ? slot.getStartTime() : "";
            }));

            for (Appointment a : apps) {
                appointmentsModel.addRow(buildAppointmentRow(a));
            }
        }
    }

    private Object[] buildAppointmentRow(Appointment a) {
        ScheduleSlot slot = appointmentService.getSlotById(a.getSlotId());
        String date = (slot != null) ? slot.getSlotDate() : "";
        String time = (slot != null) ? slot.getStartTime() + " - " + slot.getEndTime() : "";

        return new Object[] {
            a.getAppointmentId(),
            resolveDoctorName(a.getDoctorId()),
            resolvePatientName(a.getPatientId()),
            date,
            time,
            a.getStatus(),
            a.getReasonForVisit()
        };
    }

    public static String resolvePatientName(String patientIdOrName) {
        if (patientIdOrName == null || patientIdOrName.trim().isEmpty()) {
            return "N/A";
        }

        for (User user : DatabaseStore.getInstance().getUsers().values()) {
            if (patientIdOrName.equalsIgnoreCase(user.getUserId()) || patientIdOrName.equalsIgnoreCase(user.getFullName())) {
                return user.getFullName();
            }
        }
        return patientIdOrName;
    }

    public static String resolveDoctorName(String doctorIdOrName) {
        if (doctorIdOrName == null || doctorIdOrName.trim().isEmpty()) {
            return "N/A";
        }

        UserService userService = new UserService();
        List<User> allUsers = userService.getAllUsers();
        if (allUsers != null) {
            for (User u : allUsers) {
                if (doctorIdOrName.equalsIgnoreCase(u.getUserId()) || doctorIdOrName.equalsIgnoreCase(u.getFullName())) {
                    return u.getFullName();
                }
            }
        }
        return doctorIdOrName;
    }

    private void handleCheckIn() {
        int row = appointmentsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an appointment.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String appId = (String) appointmentsModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(mainFrame, "Confirm patient check-in for Appointment ID: " + appId + "?",
                "Check-In Patient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.OK_OPTION) {
            if (operationService.checkInPatient(mainFrame.getCurrentUser(), appId)) {
                JOptionPane.showMessageDialog(mainFrame, "Patient Checked In successfully.");
                refreshData(mainFrame.getCurrentUser());
                dashboard.refreshDashboardData();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Check-in failed. Doctor/Admin role required.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleUpdateStatus() {
        int row = appointmentsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an appointment.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String appId = (String) appointmentsModel.getValueAt(row, 0);
        String[] statusOptions = { "SCHEDULED", "WAITING", "IN_CONSULTATION", "COMPLETED", "CANCELLED" };
        JComboBox<String> statusDropdown = new JComboBox<>(statusOptions);

        JPanel panelDialog = new JPanel(new GridLayout(2, 1, 5, 5));
        panelDialog.add(new JLabel("Select new status for " + appId + ":"));
        panelDialog.add(statusDropdown);

        int option = JOptionPane.showConfirmDialog(mainFrame, panelDialog, "Update Appointment Status",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String selectedStatus = (String) statusDropdown.getSelectedItem();
            if (selectedStatus != null) {
                statusService.updateAppointmentStatus(appId, selectedStatus);
                JOptionPane.showMessageDialog(mainFrame, "Status updated to: " + selectedStatus);
                refreshData(mainFrame.getCurrentUser());
                dashboard.refreshDashboardData();
            }
        }
    }

    private void handleOpenConsultation() {
        int row = appointmentsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an appointment to view/start consultation.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String appId = (String) appointmentsModel.getValueAt(row, 0);
        Appointment app = statusService.getAppointmentDetails(appId);
        if (app != null) {
            ClinicalConsultationDialog dialog = new ClinicalConsultationDialog(mainFrame, app, mainFrame.getCurrentUser(), operationService);
            dialog.setVisible(true);
            refreshData(mainFrame.getCurrentUser());
            dashboard.refreshDashboardData();
        }
    }

    private void handleReschedule() {
        int row = appointmentsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an appointment to reschedule.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String appId = (String) appointmentsModel.getValueAt(row, 0);
        Appointment selectedApp = appointmentService.getAppointmentById(appId);

        if (selectedApp == null) {
            JOptionPane.showMessageDialog(mainFrame, "Appointment not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String currentStatus = selectedApp.getStatus() != null ? selectedApp.getStatus().toUpperCase() : "";
        if ("CANCELLED".equals(currentStatus) || "COMPLETED".equals(currentStatus) || "IN_CONSULTATION".equals(currentStatus)) {
            JOptionPane.showMessageDialog(mainFrame, "An appointment with status '" + currentStatus + "' cannot be rescheduled.", "Reschedule Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<ScheduleSlot> availableSlots = appointmentService.getAvailableSlotsByDoctor(selectedApp.getDoctorId());
        if (availableSlots == null || availableSlots.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "No available slots found for doctor: " + resolveDoctorName(selectedApp.getDoctorId()), "Reschedule Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] slotOptions = new String[availableSlots.size()];
        for (int i = 0; i < availableSlots.size(); i++) {
            ScheduleSlot s = availableSlots.get(i);
            slotOptions[i] = s.getSlotId() + " | " + s.getSlotDate() + " " + s.getStartTime() + " - " + s.getEndTime();
        }

        JComboBox<String> slotBox = new JComboBox<>(slotOptions);
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.add(new JLabel("Select new slot for Patient " + resolvePatientName(selectedApp.getPatientId()) + ":"));
        panel.add(slotBox);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Reschedule Appointment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int selectedIndex = slotBox.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < availableSlots.size()) {
                ScheduleSlot newSlot = availableSlots.get(selectedIndex);
                if (appointmentService.rescheduleAppointment(appId, newSlot.getSlotId())) {
                    JOptionPane.showMessageDialog(mainFrame, "Appointment rescheduled successfully.");
                    refreshData(mainFrame.getCurrentUser());
                    dashboard.refreshDashboardData();
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Reschedule failed.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void handleCancelAppointment() {
        int row = appointmentsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an appointment.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String appId = (String) appointmentsModel.getValueAt(row, 0);
        Appointment app = appointmentService.getAppointmentById(appId);

        if (app != null) {
            String status = app.getStatus() != null ? app.getStatus().toUpperCase() : "";
            if ("CANCELLED".equals(status) || "COMPLETED".equals(status)) {
                JOptionPane.showMessageDialog(mainFrame, "This appointment has already been " + status.toLowerCase() + " and cannot be cancelled.", "Cancellation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        JTextField reasonField = new JTextField(20);
        JPanel panelDialog = new JPanel(new GridLayout(2, 1, 5, 5));
        panelDialog.add(new JLabel("Enter Cancellation Reason for " + appId + ":"));
        panelDialog.add(reasonField);

        int option = JOptionPane.showConfirmDialog(mainFrame, panelDialog, "Cancel Appointment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Cancellation reason is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (appointmentService.cancelAppointment(appId, reason)) {
                JOptionPane.showMessageDialog(mainFrame, "Appointment Cancelled Successfully.");
                refreshData(mainFrame.getCurrentUser());
                dashboard.refreshDashboardData();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Unable to cancel appointment.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}