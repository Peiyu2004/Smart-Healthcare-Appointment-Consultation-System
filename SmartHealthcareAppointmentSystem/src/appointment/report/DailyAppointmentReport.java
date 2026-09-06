package appointment.report;

import appointment.Appointment;
import java.time.LocalDate;
import java.util.List;

public class DailyAppointmentReport extends Report {
    private List<Appointment> appointments;
    private LocalDate targetDate;

    public DailyAppointmentReport(List<Appointment> appointments, LocalDate targetDate) {
        super();
        this.appointments = appointments;
        this.targetDate = targetDate;
    }

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append("Daily Appointment Report - ").append(targetDate).append("\n");
        sb.append("Generated on: ").append(generatedDate).append("\n");
        sb.append("--------------------------------------\n");

        int count = 0;
        for (Appointment a : appointments) {
            if (a.getDate().toLocalDate().equals(targetDate)) {
                sb.append(a.getAppointmentId()).append(" | ")
                  .append(a.getPatientName()).append(" | ")
                  .append(a.getDoctorName()).append(" | ")
                  .append(a.getStatus()).append("\n");
                count++;
            }
        }
        sb.append("--------------------------------------\n");
        sb.append("Total Appointments: ").append(count);
        return sb.toString();
    }
}