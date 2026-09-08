package com.healthcare.ui;

import com.healthcare.model.User;
import com.healthcare.service.UserService;
import com.healthcare.ui.UserManagementDialog;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private final HealthcareUI mainFrame;
    private final UserService userService;

    private final JTextField loginEmailField = new JTextField(18);
    private final JPasswordField loginPassField = new JPasswordField(18);

    public LoginPanel(HealthcareUI mainFrame, UserService userService) {
        this.mainFrame = mainFrame;
        this.userService = userService;
        setLayout(new GridBagLayout());
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Smart Healthcare System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JButton loginButton = HealthcareUI.createStyledButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        JButton registerButton = HealthcareUI.createStyledButton("Register");
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        add(loginEmailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        add(loginPassField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(loginButton, gbc);

        gbc.gridy = 4;
        add(registerButton, gbc);

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> UserManagementDialog.showRegisterDialog(mainFrame, userService));
    }

    private void handleLogin() {
        String email = loginEmailField.getText().trim();
        String pass = new String(loginPassField.getPassword()).trim();

        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Please fill in all fields.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = userService.authenticate(email, pass);
        if (user != null) {
            mainFrame.onLoginSuccess(user);
        } else {
            JOptionPane.showMessageDialog(mainFrame, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearFields() {
        loginEmailField.setText("");
        loginPassField.setText("");
    }
}