package com.healthcare.model;

public class Consultation {
    private String consultationId;
    private String appointmentId;
    private String patientName;
    private String doctorName;
    private String clinicalNotes;
    private String diagnosis;
    private String recordDate;
    private String prescription;

    // Overloaded Constructor for legacy callers without prescription
    public Consultation(String consultationId, String appointmentId, String patientName, 
                        String doctorName, String clinicalNotes, String diagnosis, String recordDate) {
        this(consultationId, appointmentId, patientName, doctorName, clinicalNotes, diagnosis, recordDate, "");
    }

    // Full Constructor
    public Consultation(String consultationId, String appointmentId, String patientName, 
                        String doctorName, String clinicalNotes, String diagnosis, String recordDate, String prescription) {
        this.consultationId = consultationId;
        this.appointmentId = appointmentId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.clinicalNotes = clinicalNotes;
        this.diagnosis = diagnosis;
        this.recordDate = recordDate;
        this.prescription = prescription != null ? prescription : "";
    }

    // Getters
    public String getConsultationId() { return consultationId; }
    public String getAppointmentId() { return appointmentId; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
    public String getClinicalNotes() { return clinicalNotes; }
    public String getDiagnosis() { return diagnosis; }
    public String getRecordDate() { return recordDate; }
    public String getPrescription() { return prescription; }

    // Setters
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    // Converts the object to an 8-part pipe-delimited string format for file writing
    public String toFileString() {
        return String.join("|",
            consultationId != null ? consultationId : "",
            appointmentId != null ? appointmentId : "",
            patientName != null ? patientName : "",
            doctorName != null ? doctorName : "",
            clinicalNotes != null ? clinicalNotes.replace("\n", " ") : "",
            diagnosis != null ? diagnosis.replace("\n", " ") : "",
            recordDate != null ? recordDate : "",
            prescription != null ? prescription.replace("\n", " ") : ""
        );
    }

    // Parses a line from consultations.txt into a Consultation object
    public static Consultation fromFileString(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < 7) {
            return null;
        }

        String rx = parts.length >= 8 ? parts[7].trim() : "";

        return new Consultation(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            parts[4].trim(),
            parts[5].trim(),
            parts[6].trim(),
            rx
        );
    }
}