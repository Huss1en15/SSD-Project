package model;

import java.time.LocalDate;

public final class LeaveRequest {
    private int id;
    private int userId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status;

    // Constructor to initialize all fields of a leave request
    public LeaveRequest(int id, int userId, String name, LocalDate startDate, LocalDate endDate, String reason, String status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
    }

    // Returns the ID of the leave request
    public int getId() { return id; }

    // Returns the user ID associated with the leave request
    public int getUserId() { return userId; }

    // Returns the name of the employee who made the request
    public String getName() { return name; }

    // Returns the start date of the leave
    public LocalDate getStartDate() { return startDate; }

    // Returns the end date of the leave
    public LocalDate getEndDate() { return endDate; }

    // Returns the reason for the leave request
    public String getReason() { return reason; }

    // Returns the current status of the leave request
    public String getStatus() { return status; }
}