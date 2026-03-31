package dao;

import model.User;
import util.SecurityUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class UserDAO {
    private UserDAO() {}

    public static User authenticate(String username, String password) throws Exception {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            System.out.println("Trying username: "+username);

            if (rs.next()) {
                System.out.println("User found");
                System.out.println("DB username: " + rs.getString("username"));
                System.out.println("Stroed password: " + rs.getString("password"));
                String storedHash = rs.getString("password");

                if (SecurityUtils.verifyPassword(password, storedHash) || password.equals(storedHash)) {
                    if (SecurityUtils.needsRehash(storedHash)) {
                        upgradePasswordHash(rs.getInt("id"), password, con);
                    }

                    int userId = rs.getInt("id");
                    return new User(
                            userId,
                            rs.getString("name"),
                            rs.getString("username"),
                            rs.getString("role"),
                            LeaveDAO.getLeaveBalanceByUser(userId)
                    );
                }
            } else {
                System.out.println("Password verification failed");
            }
        }
        return null;
    }

    public static boolean updatePassword(int userId, String oldPassword, String newPassword) throws Exception {
        String selectSql = "SELECT password FROM users WHERE id = ?";
        String updateSql = "UPDATE users SET password = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement selectStmt = con.prepareStatement(selectSql)) {

            selectStmt.setInt(1, userId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String currentHash = rs.getString("password");

                if (!SecurityUtils.verifyPassword(oldPassword, currentHash)) {
                    return false;
                }

                try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                    String newHash = SecurityUtils.hashPassword(newPassword);
                    updateStmt.setString(1, newHash);
                    updateStmt.setInt(2, userId);
                    updateStmt.executeUpdate();
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean createUser(String name, String username, String plainPassword, String role) throws Exception {
        String sql = "INSERT INTO users (name, username, password, role) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, username);
            stmt.setString(3, SecurityUtils.hashPassword(plainPassword));
            stmt.setString(4, role.toLowerCase());
            return stmt.executeUpdate() == 1;
        }
    }

    public static boolean usernameExists(String username) throws Exception {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static User getUserById(int userId) throws Exception {
        String sql = "SELECT id, name, username, role FROM users WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("role"),
                        LeaveDAO.getLeaveBalanceByUser(userId)
                );
            }
        }
        return null;
    }

    private static void upgradePasswordHash(int userId, String plainPassword, Connection con) throws Exception {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, SecurityUtils.hashPassword(plainPassword));
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }
}
