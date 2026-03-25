package controller;

import dao.ActivityLogDAO;
import dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;
import util.SecurityUtils;

public final class ResetPasswordController {

    private User currentUser;

    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void handleChangePassword() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "User not set. Cannot change password.");
            return;
        }

        String oldPass = oldPasswordField.getText() == null ? "" : oldPasswordField.getText();
        String newPass = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (oldPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "All fields are required.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.WARNING, "New passwords do not match.");
            return;
        }

        if (newPass.equals(oldPass)) {
            showAlert(Alert.AlertType.WARNING, "New password must be different from the current password.");
            return;
        }

        if (!SecurityUtils.isStrongPassword(newPass)) {
            showAlert(Alert.AlertType.WARNING,
                    "Password must be at least 8 characters long and include:\n" +
                            "- an uppercase letter\n" +
                            "- a lowercase letter\n" +
                            "- a number\n" +
                            "- a special character"
            );
            return;
        }

        try {
            boolean success = UserDAO.updatePassword(currentUser.getId(), oldPass, newPass);
            if (success) {
                ActivityLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "Changed password");
                showAlert(Alert.AlertType.INFORMATION, "Password updated successfully.");
                Stage stage = (Stage) oldPasswordField.getScene().getWindow();
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Current password is incorrect.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error updating password.");
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
