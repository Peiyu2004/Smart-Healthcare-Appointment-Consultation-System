package appointment.report;

import java.time.LocalDate;

public abstract class Report {
    protected LocalDate generatedDate;

    public Report() {
        this.generatedDate = LocalDate.now();
    }

    public abstract String generate();

    public LocalDate getGeneratedDate() {
        return generatedDate;
    }
}