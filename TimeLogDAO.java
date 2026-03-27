package dao;

import model.TimeLog;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TimeLogDAO {
    private TimeLogDAO() {}

    public static void clockIn(int userId, LocalDateTime time) throws Exception {
        if (hasOpenClockIn(userId)) {
            throw new Exception("User already has an active clock-in session.");
        }

        String name = getUserName(userId);
        String sql = "INSERT INTO time_logs (user_id, name, clock_in) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, name);
            stmt.setTimestamp(3, Timestamp.valueOf(time));
            stmt.executeUpdate();
        }
    }

    public static void clockOut(int userId, LocalDateTime clockOut) throws Exception {
        String sql = "UPDATE time_logs SET clock_out = ?, working_hours = ? WHERE user_id = ? AND clock_out IS NULL";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            LocalDateTime clockInTime = getLastClockIn(userId);
            if (clockOut.isBefore(clockInTime)) {
                throw new Exception("Clock out time cannot be before clock in time.");
            }

            Duration duration = Duration.between(clockInTime, clockOut);
            String workingHours = String.format("%02d:%02d", duration.toHours(), duration.toMinutes() % 60);
            stmt.setTimestamp(1, Timestamp.valueOf(clockOut));
            stmt.setString(2, workingHours);
            stmt.setInt(3, userId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new Exception("No open clock-in found.");
            }
        }
    }

    private static boolean hasOpenClockIn(int userId) throws Exception {
        String sql = "SELECT COUNT(*) FROM time_logs WHERE user_id = ? AND clock_out IS NULL";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static LocalDateTime getLastClockIn(int userId) throws Exception {
        String sql = "SELECT clock_in FROM time_logs WHERE user_id = ? AND clock_out IS NULL ORDER BY clock_in DESC LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp("clock_in").toLocalDateTime();
            }
            throw new Exception("No open clock-in found.");
        }
    }

    public static List<TimeLog> getAllLogs() throws Exception {
        List<TimeLog> logs = new ArrayList<>();
        String query = "SELECT * FROM time_logs ORDER BY clock_in DESC";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                logs.add(buildTimeLogFromResultSet(rs));
            }
        }
        return logs;
    }

    public static List<TimeLog> getLogsByUser(int userId) throws Exception {
        List<TimeLog> logs = new ArrayList<>();
        String query = "SELECT * FROM time_logs WHERE user_id = ? ORDER BY clock_in DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(buildTimeLogFromResultSet(rs));
            }
        }
        return logs;
    }

    private static TimeLog buildTimeLogFromResultSet(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String name = rs.getString("name");
        LocalDateTime clockIn = rs.getTimestamp("clock_in").toLocalDateTime();
        Timestamp clockOutTS = rs.getTimestamp("clock_out");
        LocalDateTime clockOut = clockOutTS != null ? clockOutTS.toLocalDateTime() : null;
        String workingHours = rs.getString("working_hours");
        return new TimeLog(userId, name, clockIn, clockOut, workingHours);
    }

    private static String getUserName(int userId) throws Exception {
        String sql = "SELECT name FROM users WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
            throw new Exception("User name not found for ID: " + userId);
        }
    }
}
