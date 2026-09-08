package com.healthcare.ui;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DatabaseStore;
import com.healthcare.service.HealthcareOperationService;
import com.healthcare.service.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueueTabPanel extends JPanel {
    private final HealthcareUI mainFrame;
    private final DashboardPanel dashboard;

    private final HealthcareOperationService operationService = new HealthcareOperationService();
    private final UserService userService = new UserService();
    private final AppointmentService appointmentService = new AppointmentService();

    private final DefaultTableModel queueModel = new HealthcareUI.NonEditableTableModel(
            new String[] { "Queue Position", "Appointment ID", "Doctor Name", "Patient Name", "Date", "Time", "Status" }, 0);
    private final JTable queueTable = new JTable(queueModel);

    private JPanel queueHeaderPanel;
    private JComboBox<String> queueDoctorFilterBox;
    private JTextField dateFilterField;
    private JLabel dateFilterLabel;
    private JButton callNextBtn;

    public QueueTabPanel(HealthcareUI mainFrame, DashboardPanel dashboard) {
        this.mainFrame = mainFrame;
        this.dashboard = dashboard;
        setLayout(new BorderLayout(5, 5));
        initComponents();
    }

    private void initComponents() {
        queueHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        queueDoctorFilterBox = new JComboBox<>();
        queueHeaderPanel.add(new JLabel("Filter Queue by Doctor:"));
        queueHeaderPanel.add(queueDoctorFilterBox);

        dateFilterLabel = new JLabel("Filter Date (YYYY-MM-DD):");
        dateFilterField = new JTextField(10);
        queueHeaderPanel.add(dateFilterLabel);
        queueHeaderPanel.add(dateFilterField);

        JButton applyDateFilterBtn = HealthcareUI.createStyledButton("Apply Date Filter");
        queueHeaderPanel.add(applyDateFilterBtn);

        queueDoctorFilterBox.addActionListener(e -> {
            User currentUser = mainFrame.getCurrentUser();
            if (currentUser != null) {
                refreshData(currentUser);
            }
        });

        applyDateFilterBtn.addActionListener(e -> {
            User currentUser = mainFrame.getCurrentUser();
            if (currentUser != null) {
                refreshData(currentUser);
            }
        });

        add(queueHeaderPanel, BorderLayout.NORTH);
        add(new JScrollPane(queueTable), BorderLayout.CENTER);

        callNextBtn = HealthcareUI.createStyledButton("Call Next Patient into Consultation");
        callNextBtn.setFont(new Font("Arial", Font.BOLD, 13));
        add(callNextBtn, BorderLayout.SOUTH);

        callNextBtn.addActionListener(e -> handleCallNext());
    }

    public void applyRolePermissions(User currentUser, boolean isStaff, boolean isAdmin) {
        callNextBtn.setVisible(isStaff);
        queueHeaderPanel.setVisible(isAdmin);
        dateFilterLabel.setVisible(isAdmin);
        dateFilterField.setVisible(isAdmin);

        if (isAdmin && currentUser != null) {
            populateDoctorFilter(currentUser);
        }
    }

    public void refreshData(User currentUser) {
        if (currentUser == null) return;

        queueModel.setRowCount(0);

        String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
        boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");
        boolean isDoctor = role.equals("DOCTOR");
        boolean isPatient = role.equals("PATIENT");

        // 1. Fetch raw active queue items
        Collection<Appointment> rawQueue = operationService.getDoctorQueue(currentUser, "");
        List<Appointment> queueList = new ArrayList<>();

        if (rawQueue != null && !rawQueue.isEmpty()) {
            queueList.addAll(rawQueue);
        } else {
            List<Appointment> allApps = new ArrayList<>(DatabaseStore.getInstance().getAppointments().values());
            for (Appointment a : allApps) {
                String st = a.getStatus() != null ? a.getStatus().toUpperCase() : "";
                if ("WAITING".equals(st) || "SCHEDULED".equals(st) || "IN_CONSULTATION".equals(st)) {
                    queueList.add(a);
                }
            }
        }

        // 2. Sort chronologically by Slot Date and Start Time
        queueList.sort(Comparator.comparing((Appointment a) -> {
            ScheduleSlot slot = appointmentService.getSlotById(a.getSlotId());
            return (slot != null && slot.getSlotDate() != null) ? slot.getSlotDate() : "";
        }).thenComparing(a -> {
            ScheduleSlot slot = appointmentService.getSlotById(a.getSlotId());
            return (slot != null && slot.getStartTime() != null) ? slot.getStartTime() : "";
        }));

        // 3. Pre-calculate true queue position per doctor based on sorted order
        Map<String, Integer> doctorQueueCounters = new HashMap<>();
        Map<String, Integer> appointmentQueuePositions = new HashMap<>();

        for (Appointment a : queueList) {
            String docKey = resolveDoctorName(a.getDoctorId());
            int currentPos = doctorQueueCounters.getOrDefault(docKey, 0) + 1;
            doctorQueueCounters.put(docKey, currentPos);
            appointmentQueuePositions.put(a.getAppointmentId(), currentPos);
        }

        // 4. Render filtered items maintaining sorted order and queue positions
        String filterDate = dateFilterField.getText().trim();

        for (Appointment a : queueList) {
            ScheduleSlot slot = appointmentService.getSlotById(a.getSlotId());
            String slotDate = (slot != null) ? slot.getSlotDate() : "";
            String slotTime = (slot != null) ? slot.getStartTime() + " - " + slot.getEndTime() : "";

            // Get true position within doctor's queue
            int trueQueuePos = appointmentQueuePositions.getOrDefault(a.getAppointmentId(), 1);

            // PATIENT filtering
            if (isPatient) {
                if (a.getPatientId() == null || !a.getPatientId().equalsIgnoreCase(currentUser.getUserId())) {
                    continue;
                }
            }

            // DOCTOR filtering
            if (isDoctor) {
                String docId = a.getDoctorId();
                String docName = resolveDoctorName(docId);
                boolean matchDocId = docId != null && docId.equalsIgnoreCase(currentUser.getUserId());
                boolean matchDocName = docName != null && docName.equalsIgnoreCase(currentUser.getFullName());
                if (!matchDocId && !matchDocName) {
                    continue;
                }
            }

            // ADMIN filtering
            if (isAdmin) {
                String selectedDoctor = (String) queueDoctorFilterBox.getSelectedItem();
                if (selectedDoctor != null && !"ALL DOCTORS".equalsIgnoreCase(selectedDoctor)) {
                    String docId = a.getDoctorId();
                    String docName = resolveDoctorName(docId);
                    boolean matchesName = docId != null && docId.equalsIgnoreCase(selectedDoctor);
                    boolean matchesResolved = docName != null && docName.equalsIgnoreCase(selectedDoctor);

                    if (!matchesName && !matchesResolved) {
                        continue;
                    }
                }

                if (!filterDate.isEmpty() && !slotDate.equalsIgnoreCase(filterDate)) {
                    continue;
                }
            }

            queueModel.addRow(new Object[] {
                trueQueuePos,
                a.getAppointmentId(),
                resolveDoctorName(a.getDoctorId()),
                AppointmentsTabPanel.resolvePatientName(a.getPatientId()),
                slotDate,
                slotTime,
                a.getStatus()
            });
        }
    }

    private void populateDoctorFilter(User currentUser) {
        Object currentSelection = queueDoctorFilterBox.getSelectedItem();
        queueDoctorFilterBox.removeAllItems();
        queueDoctorFilterBox.addItem("ALL DOCTORS");

        List<User> allUsers = userService.getAllUsers();
        boolean foundDoctors = false;
        if (allUsers != null) {
            for (User u : allUsers) {
                if (u.getRoleLabel() != null && "DOCTOR".equalsIgnoreCase(u.getRoleLabel().trim())) {
                    queueDoctorFilterBox.addItem(u.getFullName());
                    foundDoctors = true;
                }
            }
        }

        if (!foundDoctors) {
            List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(currentUser);
            if (allSlots != null) {
                for (ScheduleSlot s : allSlots) {
                    String docName = resolveDoctorName(s.getDoctorId());
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

        if (currentSelection != null) {
            queueDoctorFilterBox.setSelectedItem(currentSelection);
        }
    }

    private String resolveDoctorName(String doctorIdOrName) {
        if (doctorIdOrName == null || doctorIdOrName.trim().isEmpty()) return "N/A";

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

    private void handleCallNext() {
        User currentUser = mainFrame.getCurrentUser();
        if (currentUser == null) return;

        String selectedFilter = "";
        if (queueDoctorFilterBox.getSelectedItem() != null) {
            selectedFilter = (String) queueDoctorFilterBox.getSelectedItem();
        }

        String targetDoctor = "ALL DOCTORS".equalsIgnoreCase(selectedFilter) ? "" : selectedFilter;
        if (targetDoctor.isEmpty() && "DOCTOR".equalsIgnoreCase(currentUser.getRoleLabel())) {
            targetDoctor = currentUser.getFullName();
        }

        Appointment nextApp = operationService.callNextPatient(currentUser, targetDoctor);
        if (nextApp != null) {
            JOptionPane.showMessageDialog(mainFrame, "Calling Patient: " + AppointmentsTabPanel.resolvePatientName(nextApp.getPatientId())
                    + "\nAppointment ID: " + nextApp.getAppointmentId()
                    + "\nStatus set to IN_CONSULTATION.\nReminder Sent!");

            // Refresh queue panel UI as well as global dashboard
            refreshData(currentUser);
            dashboard.refreshDashboardData();
        } else {
            JOptionPane.showMessageDialog(mainFrame, "No patients currently waiting or checked in for consultation.");
        }
    }
}