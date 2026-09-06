package com.healthcare.model;

public class ScheduleSlot {
    private String slotId;
    private String doctorId;
    private String slotDate;
    private String startTime;
    private String endTime;
    private String mode;
    private String status;

    public ScheduleSlot(String slotId, String doctorId, String slotDate, String startTime, String endTime, String mode, String status) {
        this.slotId = slotId;
        this.doctorId = doctorId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.mode = mode;
        this.status = status;
    }

    public String getSlotId() { return slotId; }
    public String getDoctorId() { return doctorId; }
    public String getSlotDate() { return slotDate; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getMode() { return mode; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
    public boolean isAvailable() { return "AVAILABLE".equalsIgnoreCase(this.status); }
}