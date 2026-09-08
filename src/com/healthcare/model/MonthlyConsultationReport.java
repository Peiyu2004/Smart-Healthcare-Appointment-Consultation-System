package com.healthcare.model;

import java.time.Month;
import java.time.Year;
import java.util.List;

public class MonthlyConsultationReport extends Report {
    private List<Appointment> appointments;
    private Month targetMonth;
    private Year targetYear;

    public MonthlyConsultationReport(List<Appointment> appointments, Month targetMonth, Year targetYear) {
        super();
        this.appointments = appointments;
        this.targetMonth = targetMonth;
        this.targetYear = targetYear;
    }

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append("Monthly Consultation Report - ").append(targetMonth).append(" ").append(targetYear).append("\n");
        sb.append("Generated on: ").append(generatedDate).append("\n");
        sb.append("--------------------------------------\n");

        int completed = 0;
        int cancelled = 0;
        for (Appointment a : appointments) {
            if (a.getDate().getMonth() == targetMonth && Year.of(a.getDate().getYear()).equals(targetYear)) {
                if (a.getStatus().equalsIgnoreCase("Completed")) {
                    completed++;
                } else if (a.getStatus().equalsIgnoreCase("Cancelled")) {
                    cancelled++;
                }
            }
        }
        sb.append("Completed Consultations: ").append(completed).append("\n");
        sb.append("Cancelled Consultations: ").append(cancelled);
        return sb.toString();
    }
}