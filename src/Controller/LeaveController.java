package controller;

import dao.ActivityLogDAO;
import dao.LeaveDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;
import util.SecurityUtils;

import java.time.LocalDate;

public final class LeaveController {

    private User currentUser;

    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private TextArea reasonArea;
    @FXML private Button submitButton;

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void submitLeaveRequest() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "No authenticated user session.");
            return;
        }

        if (startDate.getValue() == null || endDate.getValue() == null || reasonArea.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Please fill in all fields.");
            return;
        }

        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();
        if (start.isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Start date cannot be in the past.");
            return;
        }
        if (end.isBefore(start)) {
            showAlert(Alert.AlertType.WARNING, "End date must be after or equal to start date.");
            return;
        }

        String reason = SecurityUtils.sanitizeText(reasonArea.getText());
        if (!SecurityUtils.isValidLeaveReason(reason)) {
            showAlert(Alert.AlertType.WARNING, "Reason must be 5 to 200 characters and contain only letters, numbers, spaces, and common punctuation.");
            return;
        }

        try {
            LeaveDAO.submitLeave(currentUser.getId(), start, end, reason);
            ActivityLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "Submitted leave request");
            showAlert(Alert.AlertType.INFORMATION, "Leave request submitted successfully.");
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to submit leave.");
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.showAndWait();
    }
}
