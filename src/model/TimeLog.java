package model;

import java.time.Duration;
import java.time.LocalDateTime;

public final class TimeLog {
    private int userId;
    private String name;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private String workingHours;

    // Constructor to initialize all fields of a time log entry
    public TimeLog(int userId, String name, LocalDateTime clockIn, LocalDateTime clockOut, String workingHours) {
        this.userId = userId;
        this.name = name;
        this.clockIn = clockIn;
        this.clockOut = clockOut;
        this.workingHours = workingHours;
    }

    // Returns the user ID associated with this time log
    public int getUserId() {
        return userId;
    }

    // Returns the name of the employee
    public String getName() {
        return name;
    }

    // Returns the clock-in time
    public LocalDateTime getClockIn() {
        return clockIn;
    }

    // Returns the clock-out time
    public LocalDateTime getClockOut() {
        return clockOut;
    }

    // Sets the value of calculated working hours
    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    // Returns the total working hours as a formatted string (HH:mm) or "N/A" if data is incomplete
    public String getWorkingHours() {
        if (workingHours != null) {
            return workingHours;
        }

        if (clockIn != null && clockOut != null) {
            Duration duration = Duration.between(clockIn, clockOut);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            return String.format("%02d:%02d", hours, minutes);
        }

        return "N/A";
    }
}