package dao;

import dao.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public final class ActivityLogDAO {
    // Logs an activity to the activity_logs table with the current timestamp
    public static void log(int userId, String username, String role, String action) {
        String sql = "INSERT INTO activity_logs (user_id, username, role, action, timestamp) VALUES (?, ?, ?, ?, NOW())";

        // Try-with-resources block to automatically close DB resources
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set parameters for the INSERT query
            stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, role);
            stmt.setString(4, action);

            // Execute the insert operation
            stmt.executeUpdate();

            // Handle any exceptions during the logging process
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to log activity: " + e.getMessage());
        }
    }
}
