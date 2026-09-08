package com.healthcare.ui;

import com.healthcare.model.User;
import com.healthcare.service.UserService;
import com.healthcare.service.HealthcareOperationService;

import javax.swing.*;
import java.awt.*;

public class HealthcareUI extends JFrame {
    private final UserService userService = new UserService();
    private User currentUser = null;
    private HealthcareOperationService healthcareOperationService = new HealthcareOperationService();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private LoginPanel loginPanel;
    private DashboardPanel dashboardPanel;

    public HealthcareUI() {
        // Global Styling
        UIManager.put("Button.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("Button.select", new Color(180, 205, 235));

        setTitle("Smart Healthcare Appointment & Consultation System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initViews();

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
    }

    private void initViews() {
        loginPanel = new LoginPanel(this, userService);
        dashboardPanel = new DashboardPanel(this, userService);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(dashboardPanel, "DASHBOARD");
    }

    public void onLoginSuccess(User user) {
        this.currentUser = user;
        dashboardPanel.onUserLoggedIn(user);
        cardLayout.show(mainPanel, "DASHBOARD");
    }

    public void onLogout() {
        this.currentUser = null;
        loginPanel.clearFields();
        cardLayout.show(mainPanel, "LOGIN");
    }

    public User getCurrentUser() {
        return currentUser;
    }
    
    public HealthcareOperationService getHealthcareOperationService() {
        return this.healthcareOperationService;
    }

    public static JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        return btn;
    }

    public static class NonEditableTableModel extends javax.swing.table.DefaultTableModel {
        public NonEditableTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}