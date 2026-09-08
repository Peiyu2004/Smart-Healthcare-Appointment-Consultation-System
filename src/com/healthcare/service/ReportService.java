package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.PeriodAppointmentReport;
import com.healthcare.model.ReportManager;

import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private AppointmentService appointmentService;
    private ReportManager reportManager;

    public ReportService() {
        this.appointmentService = new AppointmentService();
        this.reportManager = new ReportManager();
    }

    public String generateReport(LocalDate periodStart, LocalDate periodEnd, String reportCategory) {
        List<Appointment> allAppointments = appointmentService.getAllAppointments();
        PeriodAppointmentReport report = new PeriodAppointmentReport(allAppointments, periodStart, periodEnd, reportCategory);
        return reportManager.printReport(report);
    }
}
