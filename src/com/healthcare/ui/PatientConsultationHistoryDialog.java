package com.healthcare.ui;

import com.healthcare.model.Consultation;
import com.healthcare.model.User;
import com.healthcare.service.HealthcareOperationService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PatientConsultationHistoryDialog extends JDialog {
	private final User patient;
	private final HealthcareOperationService operationService;
	private JTable historyTable;
	private DefaultTableModel tableModel;
	private JTextArea detailsArea;

	public PatientConsultationHistoryDialog(Frame owner, User patient, HealthcareOperationService operationService) {
		super(owner, "My Consultation & Prescription History", true);
		this.patient = patient;
		this.operationService = operationService;

		setSize(800, 500);
		setLocationRelativeTo(owner);
		setLayout(new BorderLayout(10, 10));

		initComponents();
		loadConsultationData();
	}

	private void initComponents() {
		// Table for listing consultations
		String[] columns = { "Consultation ID", "Date", "Attending Doctor", "Diagnosis" };
		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		historyTable = new JTable(tableModel);
		historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		historyTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				showSelectedDetails();
			}
		});

		JScrollPane tableScroll = new JScrollPane(historyTable);
		tableScroll.setPreferredSize(new Dimension(780, 180));

		// Text area for detailed record view
		detailsArea = new JTextArea();
		detailsArea.setEditable(false);
		detailsArea.setLineWrap(true);
		detailsArea.setWrapStyleWord(true);
		detailsArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
		detailsArea.setMargin(new Insets(8, 8, 8, 8));

		JScrollPane detailsScroll = new JScrollPane(detailsArea);
		detailsScroll.setBorder(BorderFactory.createTitledBorder("Selected Record Details"));

		// Close Button
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton closeBtn = HealthcareUI.createStyledButton("Close");
		closeBtn.addActionListener(e -> dispose());
		bottomPanel.add(closeBtn);

		// Split view layout
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailsScroll);
		splitPane.setDividerLocation(180);

		add(splitPane, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadConsultationData() {
		tableModel.setRowCount(0);
		List<Consultation> records = operationService.getConsultationsForPatient(patient.getUserId());

		for (Consultation c : records) {
			tableModel.addRow(
					new Object[] { c.getConsultationId(), c.getRecordDate(), c.getDoctorName(), c.getDiagnosis() });
		}

		if (records.isEmpty()) {
			detailsArea.setText("No consultation records found for your account.");
		}
	}

	private void showSelectedDetails() {
		int selectedRow = historyTable.getSelectedRow();
		if (selectedRow == -1)
			return;

		String consultationId = (String) tableModel.getValueAt(selectedRow, 0);
		List<Consultation> records = operationService.getConsultationsForPatient(patient.getUserId());

		Consultation selected = records.stream().filter(c -> c.getConsultationId().equals(consultationId)).findFirst()
				.orElse(null);

		if (selected != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("Consultation ID : ").append(selected.getConsultationId()).append("\n");
			sb.append("Date            : ").append(selected.getRecordDate()).append("\n");
			sb.append("Doctor          : ").append(selected.getDoctorName()).append("\n");
			sb.append("Diagnosis       : ").append(selected.getDiagnosis()).append("\n");
			sb.append("-----------------------------------------------------------------------\n");
			sb.append("Clinical Notes  : \n").append(selected.getClinicalNotes()).append("\n\n");
			sb.append("Prescription / Advice : \n")
					.append(selected.getPrescription() != null && !selected.getPrescription().isEmpty()
							? selected.getPrescription()
							: "No specific prescription assigned.");
			detailsArea.setText(sb.toString());
			detailsArea.setCaretPosition(0);
		}
	}
}