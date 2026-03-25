package controller;

import dao.ActivityLogDAO;
import dao.LeaveDAO;
import dao.TimeLogDAO;
import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.LeaveRequest;
import model.TimeLog;
import model.User;

import java.time.LocalDateTime;

public final class ClockController {

    private User currentUser;

    @FXML private Button logoutButton;
    @FXML private Label profileNameLabel;
    @FXML private Label profileUsernameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label leaveBalanceLabel;
    @FXML private TableView<LeaveRequest> leaveTable;
    @FXML private TableColumn<LeaveRequest, String> startDateCol;
    @FXML private TableColumn<LeaveRequest, String> endDateCol;
    @FXML private TableColumn<LeaveRequest, String> reasonCol;
    @FXML private TableColumn<LeaveRequest, String> statusCol;
    @FXML private TableView<TimeLog> attendanceTable;
    @FXML private TableColumn<TimeLog, String> clockInCol;
    @FXML private TableColumn<TimeLog, String> clockOutCol;
    @FXML private TableColumn<TimeLog, String> workingHoursCol;

    public void setUser(User user) {
        if (user == null) {
            throw new SecurityException("Invalid user session.");
        }
        this.currentUser = user;
        loadProfile();
        loadLeaveRequests();
        loadMyAttendance();
        ActivityLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "Logged in");
    }

    @FXML
    public void loadProfile() {
        try {
            ensureAuthenticated();
            User refreshedUser = UserDAO.getUserById(currentUser.getId());
            if (refreshedUser != null) {
                currentUser = refreshedUser;
            }

            if (profileNameLabel != null) profileNameLabel.setText(currentUser.getName());
            if (profileUsernameLabel != null) profileUsernameLabel.setText(currentUser.getUsername());
            if (profileRoleLabel != null) profileRoleLabel.setText(currentUser.getRole());
            if (leaveBalanceLabel != null) leaveBalanceLabel.setText(currentUser.getLeaveBalance() + " days");
        } catch (Exception e) {
            System.err.println("Profile Load Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Could not load profile information.");
        }
    }

    @FXML
    public void handleClockIn() {
        try {
            ensureAuthenticated();
            TimeLogDAO.clockIn(currentUser.getId(), LocalDateTime.now());
            ActivityLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "Clocked In");
            showAlert(Alert.AlertType.INFORMATION, "Clock in successful.");
            loadMyAttendance();
        } catch (Exception e) {
            System.err.println("Clock In Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Clock in failed.");
        }
    }

    @FXML
    public void handleClockOut() {
        try {
            ensureAuthenticated();
            TimeLogDAO.clockOut(currentUser.getId(), LocalDateTime.now());
            ActivityLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "Clocked Out");
            showAlert(Alert.AlertType.INFORMATION, "Clock out successful.");
            loadMyAttendance();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("No open clock-in")) {
                showAlert(Alert.AlertType.ERROR, "You must clock in before clocking out.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Clock out failed.");
            }
            System.err.println("Clock Out Error: " + e.getMessage());
        }
    }

    @FXML
    public void openLeaveForm() {
        try {
            ensureAuthenticated();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leave_form.fxml"));
            Parent root = loader.load();
            LeaveController leaveController = loader.getController();
            leaveController.setUser(currentUser);
            Stage stage = new Stage();
            stage.setTitle("Submit Leave Request");
            stage.setScene(new Scene(root));
            stage.show();
            ActivityLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "Opened Leave Form");
        } catch (Exception e) {
            System.err.println("Leave Form Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Could not open leave request form.");
        }
    }

    @FXML
    public void loadLeaveRequests() {
        try {
            ensureAuthenticated();
            leaveTable.setItems(FXCollections.observableArrayList(LeaveDAO.getLeaveRequestsByUser(currentUser.getId())));
            startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
            endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
            reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        } catch (Exception e) {
            System.err.println("Leave Load Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Could not load leave requests.");
        }
    }

    @FXML
    public void loadMyAttendance() {
        try {
            ensureAuthenticated();
            attendanceTable.setItems(FXCollections.observableArrayList(TimeLogDAO.getLogsByUser(currentUser.getId())));
            clockInCol.setCellValueFactory(new PropertyValueFactory<>("clockIn"));
            clockOutCol.setCellValueFactory(new PropertyValueFactory<>("clockOut"));
            workingHoursCol.setCellValueFactory(new PropertyValueFactory<>("workingHours"));
        } catch (Exception e) {
            System.err.println("Attendance Load Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Could not load attendance records.");
        }
    }

    @FXML
    public void handlePasswordReset() {
        try {
            ensureAuthenticated();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reset_password.fxml"));
            Parent root = loader.load();
            ResetPasswordController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = new Stage();
            stage.setTitle("Reset Password");
            stage.setScene(new Scene(root));
            stage.show();
            ActivityLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "Opened Password Reset");
        } catch (Exception e) {
            System.err.println("Password Reset Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Could not open password reset form.");
        }
    }

    @FXML
    public void handleLogout() {
        try {
            ensureAuthenticated();
            ActivityLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "Logged out");
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(root, 400, 300));
            loginStage.show();
            currentStage.close();
        } catch (Exception e) {
            System.err.println("Logout Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Logout failed.");
        }
    }

    private void ensureAuthenticated() {
        if (currentUser == null) {
            throw new SecurityException("No authenticated user session.");
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
