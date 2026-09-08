package com.healthcare;

import com.healthcare.ui.HealthcareUI;
import javax.swing.SwingUtilities;

public class HealthcareApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HealthcareUI ui = new HealthcareUI();
            ui.setVisible(true);
        });
    }
}