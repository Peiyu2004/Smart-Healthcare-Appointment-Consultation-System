package com.healthcare.ui;

import com.healthcare.model.Appointment;
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

	public HealthcareUI() {
		// Globally remove focus border rectangle for all buttons (including JOptionPane)
	    UIManager.put("Button.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
	    
	    // Set global default button highlight/focus colors across Metal/System LookAndFeel
	    UIManager.put("Button.select", new Color(180, 205, 235)); // Color when clicked/pressed

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

		// Slots Table Model: Removed Doctor ID, shows Doctor Name only
		slotsModel = new NonEditableTableModel(
				new String[] { "Slot ID", "Doctor Name", "Date", "Time", "Mode", "Status" }, 0);
		slotsTable = new JTable(slotsModel);

		appointmentsModel = new NonEditableTableModel(
				new String[] { "App ID", "Doctor Name", "Patient Name", "Status", "Reason" }, 0);
		appointmentsTable = new JTable(appointmentsModel);

		queueModel = new NonEditableTableModel(
				new String[] { "Queue Position", "Appointment ID", "Patient Name", "Status" }, 0);
		queueTable = new JTable(queueModel);

		// Consultation Slots Panel
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

		// Patient Action: Book Slot
		bookBtn.addActionListener(e -> {
			int row = slotsTable.getSelectedRow();
			if (row != -1) {
				String slotId = (String) slotsModel.getValueAt(row, 0);

				// Retrieve actual slot object to get doctor ID internally
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

		// Doctor/Admin Action: Add Slot
		addSlotBtn.addActionListener(e -> {
		    boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRoleLabel()) 
		                   || "ADMINISTRATOR".equalsIgnoreCase(currentUser.getRoleLabel());

		    JTextField doctorNameField = new JTextField(15);

		    // 1. Date Selection Controls (Year, Month, Day)
		    String[] years = {"2026", "2027"};
		    String[] months = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
		    String[] days = new String[31];
		    for (int i = 1; i <= 31; i++) {
		        days[i - 1] = String.format("%02d", i);
		    }

		    JComboBox<String> yearBox = new JComboBox<>(years);
		    JComboBox<String> monthBox = new JComboBox<>(months);
		    monthBox.setSelectedItem("09"); // Default September
		    JComboBox<String> dayBox = new JComboBox<>(days);
		    dayBox.setSelectedItem("10");   // Default 10th

		    JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		    datePanel.add(yearBox);
		    datePanel.add(new JLabel("-"));
		    datePanel.add(monthBox);
		    datePanel.add(new JLabel("-"));
		    datePanel.add(dayBox);

		    // 2. Time Selection Controls (Start Time & End Time)
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

		    // Layout Panel
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
		        // Construct selected Date string YYYY-MM-DD
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

		// Doctor/Admin Action: Update Slot
		updateSlotBtn.addActionListener(e -> {
			int row = slotsTable.getSelectedRow();
			if (row != -1) {
				String slotId = (String) slotsModel.getValueAt(row, 0);
				JTextField dateField = new JTextField((String) slotsModel.getValueAt(row, 2), 10);
				JTextField startField = new JTextField("09:00", 10);
				JTextField endField = new JTextField("10:00", 10);
				JComboBox<String> modeBox = new JComboBox<>(new String[] { "IN_PERSON", "TELECONSULTATION" });
				JComboBox<String> statusBox = new JComboBox<>(new String[] { "AVAILABLE", "BLOCKED", "BOOKED" });

				JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
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
					boolean updated = operationService.updateScheduleSlot(currentUser, slotId,
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
				JOptionPane.showMessageDialog(this, "Please select a slot to update.");
			}
		});

		// Doctor/Admin Action: Delete Slot
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

		// 3. Queue Management Tab
		JPanel queuePanel = new JPanel(new BorderLayout());
		queuePanel.add(new JScrollPane(queueTable), BorderLayout.CENTER);

		JButton callNextBtn = createStyledButton("Call Next Patient into Consultation");
		callNextBtn.setFont(new Font("Arial", Font.BOLD, 13));
		queuePanel.add(callNextBtn, BorderLayout.SOUTH);
		tabbedPane.addTab("Consultation Queue (UC005)", queuePanel);

		callNextBtn.addActionListener(e -> {
			if (currentUser == null)
				return;
			Appointment nextApp = operationService.callNextPatient(currentUser, currentUser.getUserId());
			if (nextApp != null) {
				JOptionPane.showMessageDialog(this,
						"Calling Patient ID: " + nextApp.getPatientId() + "\nAppointment ID: "
								+ nextApp.getAppointmentId() + "\nStatus set to IN_CONSULTATION.\nReminder Sent!");
				refreshDashboardData();
			} else {
				JOptionPane.showMessageDialog(this, "No patients currently waiting in queue or access denied.");
			}
		});

		panel.add(tabbedPane, BorderLayout.CENTER);
		return panel;
	}

	private void applyRolePermissions() {
		if (currentUser == null)
			return;

		String role = currentUser.getRoleLabel() != null ? currentUser.getRoleLabel().toUpperCase() : "PATIENT";
		userInfoLabel.setText("Logged in as: " + currentUser.getFullName() + " (" + role + ")");

		boolean isStaff = role.equals("DOCTOR") || role.equals("ADMIN") || role.equals("ADMINISTRATOR");

		// Doctors/Admins cannot book appointments; Patients cannot manage slots
		bookBtn.setVisible(!isStaff);
		addSlotBtn.setVisible(isStaff);
		updateSlotBtn.setVisible(isStaff);
		deleteSlotBtn.setVisible(isStaff);

		// Hide/Show Queue Management tab based on role
		if (!isStaff && tabbedPane.getTabCount() == 3) {
			tabbedPane.removeTabAt(2);
		} else if (isStaff && tabbedPane.getTabCount() < 3) {
			JPanel queuePanel = new JPanel(new BorderLayout());
			queuePanel.add(new JScrollPane(queueTable), BorderLayout.CENTER);
			JButton callNextBtn = createStyledButton("Call Next Patient into Consultation");
			callNextBtn.addActionListener(e -> {
				Appointment nextApp = operationService.callNextPatient(currentUser, currentUser.getUserId());
				if (nextApp != null) {
					JOptionPane.showMessageDialog(this,
							"Calling Patient ID: " + nextApp.getPatientId() + "\nStatus updated to IN_CONSULTATION.");
					refreshDashboardData();
				} else {
					JOptionPane.showMessageDialog(this, "No patients currently waiting in queue.");
				}
			});
			queuePanel.add(callNextBtn, BorderLayout.SOUTH);
			tabbedPane.addTab("Consultation Queue (UC005)", queuePanel);
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
	        // Admin View: Display ALL slots across all doctors
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
	        // Doctor View: Display ONLY slots matching the logged-in doctor's name
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
	        // Patient View: Display ONLY available ("SCHEDULED") slots
	        List<ScheduleSlot> availableSlots = appointmentService.getAvailableSlots();
	        if (availableSlots != null) {
	            for (ScheduleSlot s : availableSlots) {
	                if ("SCHEDULED".equalsIgnoreCase(s.getStatus())) {
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

	    // Refresh Queue Table (Doctors / Admins only)
	    if (isDoctor || isAdmin) {
	        queueModel.setRowCount(0);
	        Queue<Appointment> queue = operationService.getDoctorQueue(currentUser, currentUser.getUserId());
	        if (queue != null) {
	            int queuePosition = 1;
	            for (Appointment a : queue) {
	                queueModel.addRow(new Object[] { 
	                    queuePosition++, a.getAppointmentId(), a.getPatientId(), a.getStatus() 
	                });
	            }
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