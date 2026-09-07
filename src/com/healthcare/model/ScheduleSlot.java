package com.healthcare.model;

public class ScheduleSlot {
	private String slotId;
	private String doctorId;
	private String slotDate;
	private String startTime;
	private String endTime;
	private String mode;
	private String status;

	public ScheduleSlot() {
	}

	public ScheduleSlot(String slotId, String doctorId, String slotDate, String startTime, String endTime, String mode,
			String status) {
		this.slotId = slotId;
		this.doctorId = doctorId;
		this.slotDate = slotDate;
		this.startTime = startTime;
		this.endTime = endTime;
		this.mode = mode;
		this.status = status;
	}

	// Getters
	public String getSlotId() {
		return slotId;
	}

	public String getDoctorId() {
		return doctorId;
	}

	public String getSlotDate() {
		return slotDate;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public String getMode() {
		return mode;
	}

	public String getStatus() {
		return status;
	}

	// Setters
	public void setSlotId(String slotId) {
		this.slotId = slotId;
	}

	public void setDoctorId(String doctorId) {
		this.doctorId = doctorId;
	}

	public void setSlotDate(String slotDate) {
		this.slotDate = slotDate;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	// Utility Methods
	public boolean isAvailable() {
		return "AVAILABLE".equalsIgnoreCase(this.status);
	}

	@Override
	public String toString() {
		return slotId + "|" + doctorId + "|" + slotDate + "|" + startTime + "|" + endTime + "|" + mode + "|" + status;
	}
}