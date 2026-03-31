package dao;

import model.LeaveRequest;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class LeaveDAO {

    // Submits a new leave request for the given user with start/end dates and reason
    public static void submitLeave(int userId, LocalDate start, LocalDate end, String reason) throws Exception {
        String name = getUserName(userId);

        String sql = "INSERT INTO leave_requests (user_id, name, start_date, end_date, reason, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            // Set query parameters and execute insertion
            stmt.setInt(1, userId);
            stmt.setString(2, name);
            stmt.setDate(3, Date.valueOf(start));
            stmt.setDate(4, Date.valueOf(end));
            stmt.setString(5, reason);
            stmt.setString(6, "Pending");
            stmt.executeUpdate();
        }
    }

    // Updates the status of a leave request
    public static void updateLeaveStatus(int requestId, String status) throws Exception {
        String sql = "UPDATE leave_requests SET status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            // Set the new status and request ID for update
            stmt.setString(1, status);
            stmt.setInt(2, requestId);
            stmt.executeUpdate();
        }
    }

    // Retrieves all pending leave requests from the database
    public static List<LeaveRequest> getPendingRequests() throws Exception {
        List<LeaveRequest> pending = new ArrayList<>();
        String query = "SELECT lr.*, u.name FROM leave_requests lr " +
                "JOIN users u ON lr.user_id = u.id " +
                "WHERE lr.status = ? ORDER BY lr.start_date DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setString(1, "pending");
            ResultSet rs = stmt.executeQuery();

            // Build LeaveRequest objects from result set
            while (rs.next()) {
                LeaveRequest request = new LeaveRequest(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getString("reason"),
                        rs.getString("status")
                );
                pending.add(request);
            }
        }

        return pending;
    }

    // Retrieves all leave requests submitted by a specific user
    public static List<LeaveRequest> getLeaveRequestsByUser(int userId) throws Exception {
        List<LeaveRequest> userRequests = new ArrayList<>();
        String sql = "SELECT lr.*, u.name FROM leave_requests lr " +
                "JOIN users u ON lr.user_id = u.id " +
                "WHERE lr.user_id = ? ORDER BY lr.start_date DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            // Build LeaveRequest objects from result set
            while (rs.next()) {
                LeaveRequest request = new LeaveRequest(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getString("reason"),
                        rs.getString("status")
                );
                userRequests.add(request);
            }
        }

        return userRequests;
    }

    public static int getApprovedLeaveDaysByUser(int userId) throws Exception {
        String sql = "SELECT start_date, end_date FROM leave_requests WHERE user_id = ? AND LOWER(status) = 'approved'";
        int totalDays = 0;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                LocalDate start = rs.getDate("start_date").toLocalDate();
                LocalDate end = rs.getDate("end_date").toLocalDate();
                totalDays += (int) (end.toEpochDay() - start.toEpochDay()) + 1;
            }
        }

        return totalDays;
    }

    public static int getLeaveBalanceByUser(int userId) throws Exception {
        int annualAllowance = 30;
        String configured = System.getenv("DEFAULT_ANNUAL_LEAVE_DAYS");
        if (configured != null && configured.matches("\\d+")) {
            annualAllowance = Integer.parseInt(configured);
        }

        int used = getApprovedLeaveDaysByUser(userId);
        return Math.max(0, annualAllowance - used);
    }

    // Helper method to get the name of a user based on user ID
    private static String getUserName(int userId) throws Exception {
        String sql = "SELECT name FROM users WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            } else {
                throw new Exception("User name not found for ID: " + userId);
            }
        }
    }
}