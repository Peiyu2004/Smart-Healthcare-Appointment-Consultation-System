package com.healthcare.ui;

import com.healthcare.model.Appointment;
import com.healthcare.model.Consultation;
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
	private JTabbedPane tabbedPane;
	private JPanel queuePanel;

	// Admin Queue Filter
	private JComboBox<String> queueDoctorFilterBox;
	private JPanel queueHeaderPanel;

	public HealthcareUI() {
		// Globally styling
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

		return panel;
	}

	private JPanel createDashboardPanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));

		// Header Bar
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
		userInfoLabel = new JLabel("Logged in as: User");
		userInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));

		JButton logoutBtn = createStyledButton("Logout");
		topPanel.add(userInfoLabel, BorderLayout.WEST);
		topPanel.add(logoutBtn, BorderLayout.EAST);
		panel.add(topPanel, BorderLayout.NORTH);

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

		// Book Action with OK and Cancel Buttons
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

				JTextField reasonField = new JTextField(20);
				JPanel panelDialog = new JPanel(new GridLayout(2, 1, 5, 5));
				panelDialog.add(new JLabel("Enter Reason for Visit:"));
				panelDialog.add(reasonField);

				int option = JOptionPane.showConfirmDialog(this, panelDialog, "Book Appointment - Slot: " + slotId,
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

				if (option == JOptionPane.OK_OPTION) {
					String reason = reasonField.getText().trim();
					if (!reason.isEmpty()) {
						Appointment app = appointmentService.bookAppointment(currentUser.getUserId(), doctorId, slotId,
								reason);
						if (app != null) {
							JOptionPane.showMessageDialog(this,
									"Appointment Booked Successfully! ID: " + app.getAppointmentId());
							refreshDashboardData();
						} else {
							JOptionPane.showMessageDialog(this, "Failed to book slot. Slot may no longer be AVAILABLE.",
									"Error", JOptionPane.ERROR_MESSAGE);
						}
					} else {
						JOptionPane.showMessageDialog(this, "Reason for visit is required.", "Validation Error",
								JOptionPane.WARNING_MESSAGE);
					}
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select a slot from the table.", "Selection Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		// Add Slot Action
		addSlotBtn.addActionListener(e -> {
			boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRoleLabel())
					|| "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel());

			JTextField doctorNameField = new JTextField(15);

			String[] years = { "2026", "2027" };
			String[] months = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };
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

			String[] timeSlots = { "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00",
					"14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00" };

			JComboBox<String> startBox = new JComboBox<>(timeSlots);
			startBox.setSelectedItem("09:00");
			JComboBox<String> endBox = new JComboBox<>(timeSlots);
			endBox.setSelectedItem("10:00");

			JComboBox<String> modeBox = new JComboBox<>(new String[] { "IN_PERSON", "TELECONSULTATION" });

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

			int result = JOptionPane.showConfirmDialog(this, inputPanel, "Create Schedule Slot",
					JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				String selectedDate = yearBox.getSelectedItem() + "-" + monthBox.getSelectedItem() + "-"
						+ dayBox.getSelectedItem();
				String selectedStartTime = (String) startBox.getSelectedItem();
				String selectedEndTime = (String) endBox.getSelectedItem();

				String assignedDoctorName;
				if (isAdmin) {
					assignedDoctorName = doctorNameField.getText().trim();
					if (assignedDoctorName.isEmpty()) {
						JOptionPane.showMessageDialog(this, "Doctor Name cannot be empty.", "Validation Error",
								JOptionPane.WARNING_MESSAGE);
						return;
					}
				} else {
					assignedDoctorName = currentUser.getFullName();
				}

				ScheduleSlot slot = operationService.addScheduleSlot(currentUser, assignedDoctorName, selectedDate,
						selectedStartTime, selectedEndTime, (String) modeBox.getSelectedItem());

				if (slot != null) {
					JOptionPane.showMessageDialog(this, "Slot created successfully! Assigned ID: " + slot.getSlotId());
					refreshDashboardData();
				} else {
					JOptionPane.showMessageDialog(this, "Operation failed. Authorized DOCTOR or ADMIN role required.",
							"Access Denied", JOptionPane.ERROR_MESSAGE);
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
						JOptionPane.showMessageDialog(this, "Doctor Name cannot be empty.", "Validation Error",
								JOptionPane.WARNING_MESSAGE);
						return;
					}

					boolean updated = operationService.updateScheduleSlot(currentUser, slotId, updatedDoctorName,
							dateField.getText().trim(), startField.getText().trim(), endField.getText().trim(),
							(String) modeBox.getSelectedItem(), (String) statusBox.getSelectedItem());

					if (updated) {
						JOptionPane.showMessageDialog(this, "Slot updated successfully.");
						refreshDashboardData();
					} else {
						JOptionPane.showMessageDialog(this, "Update failed. Unauthorized or invalid slot.", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select a slot to update.", "Selection Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		// Delete Slot Action
		deleteSlotBtn.addActionListener(e -> {
			int row = slotsTable.getSelectedRow();
			if (row != -1) {
				String slotId = (String) slotsModel.getValueAt(row, 0);
				int confirm = JOptionPane.showConfirmDialog(this,
						"Are you sure you want to delete slot " + slotId + "?", "Confirm Deletion",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (confirm == JOptionPane.OK_OPTION) {
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
				JOptionPane.showMessageDialog(this, "Please select a slot to delete.", "Selection Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		// 2. Appointments & Status Tracking Tab
		JPanel appPanel = new JPanel(new BorderLayout());
		appPanel.add(new JScrollPane(appointmentsTable), BorderLayout.CENTER);

		JPanel appButtonPanel = new JPanel(new FlowLayout());
		JButton checkInBtn = createStyledButton("Check-In Patient");
		JButton updateStatusBtn = createStyledButton("Update Status");
		JButton openConsultBtn = createStyledButton("Open Consultation");
		JButton cancelBtn = createStyledButton("Cancel Selected Appointment");

		appButtonPanel.add(checkInBtn);
		appButtonPanel.add(updateStatusBtn);
		appButtonPanel.add(openConsultBtn);
		appButtonPanel.add(cancelBtn);
		appPanel.add(appButtonPanel, BorderLayout.SOUTH);

		tabbedPane.addTab("Appointments & Tracking", appPanel);

		checkInBtn.addActionListener(e -> {
			int row = appointmentsTable.getSelectedRow();
			if (row != -1) {
				String appId = (String) appointmentsModel.getValueAt(row, 0);
				int confirm = JOptionPane.showConfirmDialog(this,
						"Confirm patient check-in for Appointment ID: " + appId + "?", "Check-In Patient",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (confirm == JOptionPane.OK_OPTION) {
					if (operationService.checkInPatient(currentUser, appId)) {
						JOptionPane.showMessageDialog(this, "Patient Checked In. Status updated to WAITING.");
						refreshDashboardData();
					} else {
						JOptionPane.showMessageDialog(this, "Check-in failed. Doctor/Admin role required.", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select an appointment.", "Selection Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		// Update Status with OK and Cancel Buttons
		updateStatusBtn.addActionListener(e -> {
			int row = appointmentsTable.getSelectedRow();
			if (row != -1) {
				String appId = (String) appointmentsModel.getValueAt(row, 0);
				String[] statusOptions = { "SCHEDULED", "WAITING", "IN_CONSULTATION", "COMPLETED", "CANCELLED" };
				JComboBox<String> statusDropdown = new JComboBox<>(statusOptions);

				JPanel panelDialog = new JPanel(new GridLayout(2, 1, 5, 5));
				panelDialog.add(new JLabel("Select new status for " + appId + ":"));
				panelDialog.add(statusDropdown);

				int option = JOptionPane.showConfirmDialog(this, panelDialog, "Update Appointment Status",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

				if (option == JOptionPane.OK_OPTION) {
					String selectedStatus = (String) statusDropdown.getSelectedItem();
					if (selectedStatus != null) {
						statusService.updateAppointmentStatus(appId, selectedStatus);
						JOptionPane.showMessageDialog(this, "Status updated to: " + selectedStatus);
						refreshDashboardData();
					}
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select an appointment.", "Selection Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		openConsultBtn.addActionListener(e -> {
			int row = appointmentsTable.getSelectedRow();
			if (row != -1) {
				String appId = (String) appointmentsModel.getValueAt(row, 0);
				Appointment app = statusService.getAppointmentDetails(appId);
				if (app != null) {
					openConsultationDialog(app);
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select an appointment to view/start consultation.",
						"Selection Required", JOptionPane.WARNING_MESSAGE);
			}
		});

		// Cancel Appointment with OK and Cancel Buttons
		cancelBtn.addActionListener(e -> {
			int row = appointmentsTable.getSelectedRow();
			if (row != -1) {
				String appId = (String) appointmentsModel.getValueAt(row, 0);

				JTextField reasonField = new JTextField(20);
				JPanel panelDialog = new JPanel(new GridLayout(2, 1, 5, 5));
				panelDialog.add(new JLabel("Enter Cancellation Reason for " + appId + ":"));
				panelDialog.add(reasonField);

				int option = JOptionPane.showConfirmDialog(this, panelDialog, "Cancel Appointment",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

				if (option == JOptionPane.OK_OPTION) {
					String reason = reasonField.getText().trim();
					if (!reason.isEmpty()) {
						if (appointmentService.cancelAppointment(appId, reason)) {
							JOptionPane.showMessageDialog(this, "Appointment Cancelled.");
							refreshDashboardData();
						}
					} else {
						JOptionPane.showMessageDialog(this, "Cancellation reason is required.", "Validation Error",
								JOptionPane.WARNING_MESSAGE);
					}
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select an appointment.", "Selection Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		// 3. Queue Management Panel
		queuePanel = new JPanel(new BorderLayout(5, 5));

		queueHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		queueDoctorFilterBox = new JComboBox<>();
		queueHeaderPanel.add(new JLabel("Filter Queue by Doctor:"));
		queueHeaderPanel.add(queueDoctorFilterBox);

		queueDoctorFilterBox.addActionListener(e -> {
			if (currentUser != null && ("ADMIN".equalsIgnoreCase(currentUser.getRoleLabel())
					|| "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel()))) {
				refreshQueueTable();
			}
		});

		queuePanel.add(queueHeaderPanel, BorderLayout.NORTH);
		queuePanel.add(new JScrollPane(queueTable), BorderLayout.CENTER);

		JButton callNextBtn = createStyledButton("Call Next Patient into Consultation");
		callNextBtn.setFont(new Font("Arial", Font.BOLD, 13));
		queuePanel.add(callNextBtn, BorderLayout.SOUTH);

		callNextBtn.addActionListener(e -> {
			if (currentUser == null)
				return;

			String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
			boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");

			String targetDoctor = currentUser.getFullName();
			if (isAdmin && queueDoctorFilterBox.getSelectedItem() != null) {
				String selectedFilter = (String) queueDoctorFilterBox.getSelectedItem();
				targetDoctor = "ALL DOCTORS".equalsIgnoreCase(selectedFilter) ? "" : selectedFilter;
			}

			Appointment nextApp = operationService.callNextPatient(currentUser, targetDoctor);
			if (nextApp != null) {
				JOptionPane.showMessageDialog(this, "Calling Patient: " + nextApp.getPatientId() + "\nAppointment ID: "
						+ nextApp.getAppointmentId() + "\nStatus set to IN_CONSULTATION.\nReminder Sent!");
				refreshDashboardData();
			} else {
				JOptionPane.showMessageDialog(this, "No patients currently waiting in queue or access denied.");
			}
		});

		panel.add(tabbedPane, BorderLayout.CENTER);
		return panel;
	}

	// Enhanced Consultation Dialog with Submit (OK) and Cancel options
	private void openConsultationDialog(Appointment app) {
		JDialog dialog = new JDialog(this, "Clinical Consultation - Appointment ID: " + app.getAppointmentId(), true);
		dialog.setSize(850, 650);
		dialog.setLocationRelativeTo(this);
		dialog.setLayout(new BorderLayout(10, 10));

		// 1. Patient History Section (with Text Wrapping)
		List<Consultation> history = operationService.getPatientHistory(app.getPatientId());
		JPanel historyPanel = new JPanel(new BorderLayout());
		historyPanel.setBorder(BorderFactory.createTitledBorder("Patient Clinical History"));

		if (history.isEmpty()) {
			JLabel firstVisitBanner = new JLabel(" FIRST VISIT - NO PRIOR CLINICAL RECORDS FOUND",
					SwingConstants.CENTER);
			firstVisitBanner.setOpaque(true);
			firstVisitBanner.setBackground(new Color(255, 230, 230));
			firstVisitBanner.setForeground(Color.RED);
			firstVisitBanner.setFont(new Font("Arial", Font.BOLD, 13));
			firstVisitBanner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			historyPanel.add(firstVisitBanner, BorderLayout.CENTER);
		} else {
			JTextArea historyArea = new JTextArea(6, 50);
			historyArea.setEditable(false);
			historyArea.setLineWrap(true);
			historyArea.setWrapStyleWord(true);
			historyArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

			StringBuilder sb = new StringBuilder();
			for (Consultation c : history) {
				sb.append("----------------------------------------------------------------------------------------\n")
						.append("Consultation ID: ").append(c.getConsultationId()).append(" | Date: ")
						.append(c.getRecordDate()).append(" | Attending Doctor: ").append(c.getDoctorName())
						.append("\n").append("• Diagnosis: ").append(c.getDiagnosis()).append("\n")
						.append("• Clinical Notes & Prescription: ").append(c.getClinicalNotes()).append("\n");
			}
			historyArea.setText(sb.toString());
			historyArea.setCaretPosition(0);

			JScrollPane historyScroll = new JScrollPane(historyArea);
			historyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			historyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			historyPanel.add(historyScroll, BorderLayout.CENTER);
		}

		// 2. Clinical Entry Form with Structured Layout
		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBorder(BorderFactory.createTitledBorder("Active Consultation Entry"));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 8, 6, 8);
		gbc.fill = GridBagConstraints.BOTH;

		// Clinical Notes Area
		JTextArea clinicalNotesArea = createWrappedTextArea(3, 40);
		JScrollPane notesScroll = new JScrollPane(clinicalNotesArea);

		// Diagnosis Area (Medical Condition)
		JTextArea diagnosisArea = createWrappedTextArea(2, 40);
		JScrollPane diagnosisScroll = new JScrollPane(diagnosisArea);

		// Prescription & Treatment Plan Area
		JTextArea prescriptionArea = createWrappedTextArea(3, 40);
		JScrollPane prescriptionScroll = new JScrollPane(prescriptionArea);

		// Populate existing values if present
		Consultation existing = operationService.getConsultationByAppointment(app.getAppointmentId());
		if (existing != null) {
			diagnosisArea.setText(existing.getDiagnosis());
			clinicalNotesArea.setText(existing.getClinicalNotes());
		}

		// --- GridBag Alignment ---
		// Primary Diagnosis Label & Text
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.2;
		gbc.weighty = 0;
		formPanel.add(new JLabel("Primary Diagnosis*:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.8;
		gbc.weighty = 0.25;
		formPanel.add(diagnosisScroll, gbc);

		// Clinical Notes Label & Text
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.2;
		gbc.weighty = 0;
		formPanel.add(new JLabel("Clinical Notes*:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.8;
		gbc.weighty = 0.35;
		formPanel.add(notesScroll, gbc);

		// Prescription / Advice Label & Text
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0.2;
		gbc.weighty = 0;
		formPanel.add(new JLabel("Prescription / Advice:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.8;
		gbc.weighty = 0.40;
		formPanel.add(prescriptionScroll, gbc);

		// 3. Action Buttons (Submit & Cancel)
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
		JButton submitBtn = createStyledButton("Submit & Complete Consultation");
		submitBtn.setFont(new Font("Arial", Font.BOLD, 13));

		JButton dialogCancelBtn = createStyledButton("Cancel");
		dialogCancelBtn.setFont(new Font("Arial", Font.PLAIN, 13));

		btnPanel.add(submitBtn);
		btnPanel.add(dialogCancelBtn);

		// Cancel Action
		dialogCancelBtn.addActionListener(e -> dialog.dispose());

		// Complete Consultation Event Handling
		submitBtn.addActionListener(e -> {
			String diag = diagnosisArea.getText().trim();
			String notes = clinicalNotesArea.getText().trim();
			String rx = prescriptionArea.getText().trim();

			if (diag.isEmpty() || notes.isEmpty()) {
				JOptionPane.showMessageDialog(dialog,
						" Validation Error: 'Primary Diagnosis' and 'Clinical Notes' fields are required!",
						"Validation Warning", JOptionPane.WARNING_MESSAGE);
				return;
			}

			// Combine clinical notes and prescription info cleanly into clinical record
			String combinedNotes = notes;
			if (!rx.isEmpty()) {
				combinedNotes += " [Prescription/Advice: " + rx + "]";
			}

			boolean success = operationService.saveConsultationRecord(currentUser, app.getAppointmentId(),
					combinedNotes, diag);

			if (success) {
				JOptionPane.showMessageDialog(dialog,
						"Consultation record saved successfully and status updated to COMPLETED.");
				dialog.dispose();
				refreshDashboardData();
			} else {
				JOptionPane.showMessageDialog(dialog, "Failed to save record. Ensure you have authorized permissions.",
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		});

		// Add sub-panels to dialog container
		dialog.add(historyPanel, BorderLayout.NORTH);
		dialog.add(formPanel, BorderLayout.CENTER);
		dialog.add(btnPanel, BorderLayout.SOUTH);
		dialog.setVisible(true);
	}

	// Helper method to instantiate auto-wrapping JTextAreas
	private JTextArea createWrappedTextArea(int rows, int cols) {
		JTextArea area = new JTextArea(rows, cols);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(new Font("SansSerif", Font.PLAIN, 12));
		return area;
	}

	private void applyRolePermissions() {
		if (currentUser == null)
			return;

		String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
		userInfoLabel.setText("Logged in as: " + currentUser.getFullName() + " (" + role + ")");

		boolean isStaff = role.equals("DOCTOR") || role.equals("ADMIN") || role.equals("ADMINISTRATOR");
		boolean isAdmin = role.equals("ADMIN") || role.equals("ADMINISTRATOR");

		// Doctors/Admins manage slots; Patients book slots
		bookBtn.setVisible(!isStaff);
		addSlotBtn.setVisible(isStaff);
		updateSlotBtn.setVisible(isStaff);
		deleteSlotBtn.setVisible(isStaff);

		// Show Admin Queue Dropdown Filter only if Administrator
		queueHeaderPanel.setVisible(isAdmin);

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
					slotsModel.addRow(new Object[] { s.getSlotId(), s.getDoctorId(), s.getSlotDate(),
							s.getStartTime() + " - " + s.getEndTime(), s.getMode(), s.getStatus() });
				}
			}
		} else if (isDoctor) {
			List<ScheduleSlot> allSlots = operationService.getAllScheduleSlots(currentUser);
			if (allSlots != null) {
				for (ScheduleSlot s : allSlots) {
					if (currentUser.getFullName().equalsIgnoreCase(s.getDoctorId())
							|| currentUser.getUserId().equalsIgnoreCase(s.getDoctorId())) {
						slotsModel.addRow(new Object[] { s.getSlotId(), s.getDoctorId(), s.getSlotDate(),
								s.getStartTime() + " - " + s.getEndTime(), s.getMode(), s.getStatus() });
					}
				}
			}
		} else { // Patient View
			List<ScheduleSlot> availableSlots = appointmentService.getAvailableSlots();
			for (ScheduleSlot s : availableSlots) {
				slotsModel.addRow(new Object[] { s.getSlotId(), s.getDoctorId(), s.getSlotDate(),
						s.getStartTime() + " - " + s.getEndTime(), s.getMode(), s.getStatus() });
			}
		}

		// Refresh Appointments Table
		appointmentsModel.setRowCount(0);

		if (isAdmin) {
			List<Appointment> allApps = statusService.getDoctorAppointments("");
			for (Appointment a : allApps) {
				appointmentsModel.addRow(new Object[] { a.getAppointmentId(), a.getDoctorId(), a.getPatientId(),
						a.getStatus(), a.getReasonForVisit() });
			}
		} else if (isDoctor) {
			List<Appointment> docApps = statusService.getDoctorAppointments(currentUser.getFullName());
			for (Appointment a : docApps) {
				appointmentsModel.addRow(new Object[] { a.getAppointmentId(), a.getDoctorId(), a.getPatientId(),
						a.getStatus(), a.getReasonForVisit() });
			}
		} else { // Patient View
			List<Appointment> patientApps = statusService.getPatientAppointments(currentUser.getUserId());
			for (Appointment a : patientApps) {
				appointmentsModel.addRow(new Object[] { a.getAppointmentId(), a.getDoctorId(), a.getPatientId(),
						a.getStatus(), a.getReasonForVisit() });
			}
		}

		// Refresh Queue Table if Staff
		if (isAdmin || isDoctor) {
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
		int pos = 1;
		for (Appointment a : queue) {
			queueModel.addRow(
					new Object[] { pos++, a.getAppointmentId(), a.getDoctorId(), a.getPatientId(), a.getStatus() });
		}
	}

	private JButton createStyledButton(String text) {
		JButton btn = new JButton(text);
		btn.setFont(new Font("Arial", Font.PLAIN, 12));
		return btn;
	}

	// Helper table model to make table cells non-editable
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