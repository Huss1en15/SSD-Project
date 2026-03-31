package controller;

import dao.ActivityLogDAO;
import dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;
import util.SecurityUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class LoginController {
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_DURATION_SECONDS = 60;
    private static final Map<String, Integer> FAILED_ATTEMPTS = new HashMap<>();
    private static final Map<String, Instant> LOCKED_UNTIL = new HashMap<>();

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please enter both username and password.");
            return;
        }

        if (!SecurityUtils.isValidUsername(username)) {
            showAlert(Alert.AlertType.WARNING, "Username must be 4 to 20 characters and contain only letters, numbers, and underscores.");
            return;
        }

        if (isLocked(username)) {
            showAlert(Alert.AlertType.ERROR, "This account is temporarily locked. Please wait 1 minute and try again.");
            return;
        }

        try {
            User user = UserDAO.authenticate(username, password);
            if (user != null) {
                resetFailureTracking(username);

                if (!SecurityUtils.isStrongPassword(password)) {
                    showAlert(Alert.AlertType.ERROR,
                            "Your password is too weak. Please reset it after login for better security.");
                }

                openNextScreen(user);
                ActivityLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "Successful login");
            } else {
                registerFailure(username);
                showAlert(Alert.AlertType.ERROR, "Invalid credentials.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Login failed. Please try again.");
        }
    }

    private void openNextScreen(User user) throws Exception {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        Parent root;

        if ("admin".equalsIgnoreCase(user.getRole())) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/admin_panel.fxml"));
            root = loader.load();
            AdminController adminController = loader.getController();
            adminController.setCurrentAdmin(user);
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Admin Panel");
        } else {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/dashboard.fxml"));
            root = loader.load();
            ClockController controller = loader.getController();
            controller.setUser(user);
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Employee Time Tracking");
        }

        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    private boolean isLocked(String username) {
        Instant lockedUntil = LOCKED_UNTIL.get(username);
        if (lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(lockedUntil)) {
            LOCKED_UNTIL.remove(username);
            FAILED_ATTEMPTS.remove(username);
            return false;
        }
        return true;
    }

    private void registerFailure(String username) {
        int attempts = FAILED_ATTEMPTS.getOrDefault(username, 0) + 1;
        FAILED_ATTEMPTS.put(username, attempts);
        if (attempts >= MAX_ATTEMPTS) {
            LOCKED_UNTIL.put(username, Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            FAILED_ATTEMPTS.remove(username);
        }
    }

    private void resetFailureTracking(String username) {
        FAILED_ATTEMPTS.remove(username);
        LOCKED_UNTIL.remove(username);
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
