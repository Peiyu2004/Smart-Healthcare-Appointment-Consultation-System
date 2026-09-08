package com.healthcare.ui;

import com.healthcare.model.User;
import com.healthcare.service.UserService;
import com.healthcare.ui.UserManagementDialog;
import com.healthcare.ui.AppointmentsTabPanel;
import com.healthcare.ui.QueueTabPanel;
import com.healthcare.ui.SlotsTabPanel;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private final HealthcareUI mainFrame;
    private final UserService userService;
    private User currentUser;

    private JLabel userInfoLabel;
    private JButton profileBtn;
    private JButton myRecordsBtn;
    private JButton manageUsersBtn;
    private JButton generateReportBtn;

    private JTabbedPane tabbedPane;
    private SlotsTabPanel slotsTabPanel;
    private AppointmentsTabPanel appointmentsTabPanel;
    private QueueTabPanel queueTabPanel;

    public DashboardPanel(HealthcareUI mainFrame, UserService userService) {
        this.mainFrame = mainFrame;
        this.userService = userService;
        setLayout(new BorderLayout(10, 10));
        initComponents();
    }

    private void initComponents() {
        // Header Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        userInfoLabel = new JLabel("Logged in as: User");
        userInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        profileBtn = HealthcareUI.createStyledButton("My Profile");
        myRecordsBtn = HealthcareUI.createStyledButton("My Medical Records");
        manageUsersBtn = HealthcareUI.createStyledButton("Manage Users");
        generateReportBtn = HealthcareUI.createStyledButton("Generate Report");
        JButton logoutBtn = HealthcareUI.createStyledButton("Logout");

        topButtons.add(profileBtn);
        topButtons.add(myRecordsBtn);
        topButtons.add(manageUsersBtn);
        topButtons.add(generateReportBtn);
        topButtons.add(logoutBtn);

        topPanel.add(userInfoLabel, BorderLayout.WEST);
        topPanel.add(topButtons, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        profileBtn.addActionListener(e -> UserManagementDialog.showProfileDialog(mainFrame, userService, currentUser, this::onProfileUpdated));
        
        // Open patient consultation & prescription history dialog
        myRecordsBtn.addActionListener(e -> {
            PatientConsultationHistoryDialog historyDialog = new PatientConsultationHistoryDialog(
                    mainFrame,
                    currentUser,
                    mainFrame.getHealthcareOperationService()
            );
            historyDialog.setVisible(true);
        });

        manageUsersBtn.addActionListener(e -> UserManagementDialog.showManageUsersDialog(mainFrame, userService, currentUser));
        generateReportBtn.addActionListener(e -> UserManagementDialog.showGenerateReportDialog(mainFrame));
        logoutBtn.addActionListener(e -> mainFrame.onLogout());

        // Sub-tabs Initialization
        tabbedPane = new JTabbedPane();
        slotsTabPanel = new SlotsTabPanel(mainFrame, this);
        appointmentsTabPanel = new AppointmentsTabPanel(mainFrame, this);
        queueTabPanel = new QueueTabPanel(mainFrame, this);

        tabbedPane.addTab("Consultation Slots", slotsTabPanel);
        tabbedPane.addTab("Appointments & Tracking", appointmentsTabPanel);
        tabbedPane.addTab("Consultation Queue", queueTabPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void onUserLoggedIn(User user) {
        this.currentUser = user;
        applyRolePermissions();
        refreshDashboardData();
    }

    public void applyRolePermissions() {
        if (currentUser == null) return;

        String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
        userInfoLabel.setText("Logged in as: " + currentUser.getFullName() + " (" + role + ")");

        boolean isStaff = role.equals("DOCTOR") || role.equals("ADMIN") || role.equals("ADMINISTRATOR");
        boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");
        boolean isPatient = role.equals("PATIENT");

        slotsTabPanel.applyRolePermissions(currentUser, isStaff);
        appointmentsTabPanel.applyRolePermissions(isStaff);

        // Visibility rules based on role
        myRecordsBtn.setVisible(isPatient);
        manageUsersBtn.setVisible(isAdmin);
        generateReportBtn.setVisible(isAdmin);
        queueTabPanel.applyRolePermissions(currentUser, isStaff, isAdmin);
    }

    public void refreshDashboardData() {
        if (currentUser == null) return;
        slotsTabPanel.refreshData(currentUser);
        appointmentsTabPanel.refreshData(currentUser);
        queueTabPanel.refreshData(currentUser);
    }

    private void onProfileUpdated(User updatedUser) {
        this.currentUser = updatedUser;
        applyRolePermissions();
    }
}