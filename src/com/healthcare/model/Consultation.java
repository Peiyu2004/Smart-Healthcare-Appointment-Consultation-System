package com.healthcare.model;

public class Consultation {
    private String consultationId;
    private String appointmentId;
    private String startTime;
    private String endTime;
    private String clinicalNotes;
    private String diagnosis;
    private String status;

    public Consultation(String consultationId, String appointmentId, String startTime, String endTime, String clinicalNotes, String diagnosis, String status) {
        this.consultationId = consultationId;
        this.appointmentId = appointmentId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.clinicalNotes = clinicalNotes;
        this.diagnosis = diagnosis;
        this.status = status;
    }

    public String getConsultationId() { return consultationId; }
    public String getAppointmentId() { return appointmentId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getClinicalNotes() { return clinicalNotes; }
    public String getDiagnosis() { return diagnosis; }
    public String getStatus() { return status; }
}