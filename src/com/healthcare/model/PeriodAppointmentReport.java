package com.healthcare.model;

import java.time.LocalDate;
import java.util.List;

public class PeriodAppointmentReport extends Report {
    private List<Appointment> appointments;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String reportCategory;

    public PeriodAppointmentReport(List<Appointment> appointments, LocalDate periodStart, LocalDate periodEnd, String reportCategory) {
        super();
        this.appointments = appointments;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.reportCategory = reportCategory;
    }

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append("Appointment Report - ").append(reportCategory).append("\n");
        sb.append("Period: ").append(periodStart).append(" to ").append(periodEnd).append("\n");
        sb.append("Generated on: ").append(generatedDate).append("\n");
        sb.append("--------------------------------------\n");

        int scheduled = 0;
        int completed = 0;
        int cancelled = 0;
        int matchedCount = 0;

        for (Appointment a : appointments) {
            LocalDate bookedDate = a.getDate().toLocalDate();
            boolean inRange = !bookedDate.isBefore(periodStart) && !bookedDate.isAfter(periodEnd);
            if (inRange) {
                matchedCount++;
                String status = a.getStatus() == null ? "" : a.getStatus().toUpperCase();
                if (status.equals("SCHEDULED") || status.equals("RESCHEDULED")) {
                    scheduled++;
                } else if (status.equals("COMPLETED")) {
                    completed++;
                } else if (status.equals("CANCELLED")) {
                    cancelled++;
                }
            }
        }

        if (matchedCount == 0) {
            sb.append("No data found for selected timeframe");
            return sb.toString();
        }

        sb.append("Scheduled: ").append(scheduled).append("\n");
        sb.append("Completed: ").append(completed).append("\n");
        sb.append("Cancelled: ").append(cancelled).append("\n");
        sb.append("--------------------------------------\n");
        sb.append("Total Appointments: ").append(matchedCount);
        return sb.toString();
    }
}
