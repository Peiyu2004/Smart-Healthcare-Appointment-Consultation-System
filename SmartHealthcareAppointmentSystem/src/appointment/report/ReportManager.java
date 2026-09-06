package appointment.report;

public class ReportManager {
    public String printReport(Report report) {
        String output = report.generate();
        System.out.println(output);
        return output;
    }
}