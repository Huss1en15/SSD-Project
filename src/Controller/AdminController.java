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
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.LeaveRequest;
import model.TimeLog;
import model.User;
import util.SecurityUtils;

public final class AdminController {

    @FXML private TextField requestIdField;
    @FXML private TextField createNameField;
    @FXML private TextField createUsernameField;
    @FXML private PasswordField createPasswordField;
    @FXML private ComboBox<String> createRoleCombo;
    @FXML private TableView<TimeLog> attendanceTable;
    @FXML private TableColumn<TimeLog, Integer> userIdCol;
    @FXML private TableColumn<TimeLog, String> nameCol;
    @FXML private TableColumn<TimeLog, String> clockInCol;
    @FXML private TableColumn<TimeLog, String> clockOutCol;
    @FXML private TableColumn<TimeLog, String> workingHoursCol;
    @FXML private TableView<LeaveRequest> pendingLeaveTable;
    @FXML private TableColumn<LeaveRequest, Integer> leaveIdCol;
    @FXML private TableColumn<LeaveRequest, Integer> leaveUserIdCol;
    @FXML private TableColumn<LeaveRequest, String> leaveNameCol;
    @FXML private TableColumn<LeaveRequest, String> startDateCol;
    @FXML private TableColumn<LeaveRequest, String> endDateCol;
    @FXML private TableColumn<LeaveRequest, String> reasonCol;

    private User currentAdmin;

    @FXML
    public void initialize() {
        if (createRoleCombo != null) {
            createRoleCombo.setItems(FXCollections.observableArrayList("employee", "admin", "manager", "hr"));
            createRoleCombo.setValue("employee");
        }
    }

    public void setCurrentAdmin(User user) {
        SecurityUtils.requireAdmin(user.getRole());
        this.currentAdmin = user;
    }

    @FXML
    public void handleCreateUser() {
        try {
            ensureAuthorized();
            String name = SecurityUtils.sanitizeText(createNameField.getText());
            String username = SecurityUtils.sanitizeText(createUsernameField.getText());
            String password = createPasswordField.getText() == null ? "" : createPasswordField.getText();
            String role = createRoleCombo.getValue();

            if (!SecurityUtils.isValidName(name)) {
                showAlert(Alert.AlertType.WARNING, "Name must be 2 to 50 letters/spaces only.");
                return;
            }
            if (!SecurityUtils.isValidUsername(username)) {
                showAlert(Alert.AlertType.WARNING, "Username must be 4 to 20 characters and contain only letters, numbers, and underscores.");
                return;
            }
            if (!SecurityUtils.isStrongPassword(password)) {
                showAlert(Alert.AlertType.WARNING, "Password must be at least 8 characters and include uppercase, lowercase, number, and special character.");
                return;
            }
            if (!SecurityUtils.isValidRole(role)) {
                showAlert(Alert.AlertType.WARNING, "Please choose a valid role.");
                return;
            }
            if (UserDAO.usernameExists(username)) {
                showAlert(Alert.AlertType.ERROR, "This username already exists.");
                return;
            }

            boolean created = UserDAO.createUser(name, username, password, role);
            if (created) {
                logAction("Created user account for username " + username + " with role " + role);
                clearCreateUserForm();
                showAlert(Alert.AlertType.INFORMATION, "User account created successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "User account could not be created.");
            }
        } catch (SecurityException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error creating user account.");
        }
    }

    @FXML
    public void approveLeave() {
        try {
            ensureAuthorized();
            int requestId = parseRequestId();

            boolean exists = pendingLeaveTable.getItems().stream().anyMatch(req -> req.getId() == requestId);
            if (!exists) {
                showAlert(Alert.AlertType.ERROR, "Leave Request ID not found.");
                return;
            }

            LeaveDAO.updateLeaveStatus(requestId, "Approved");
            logAction("Approved leave request ID " + requestId);
            showAlert(Alert.AlertType.INFORMATION, "Leave approved.");
            requestIdField.clear();
            loadPendingLeaves();
        } catch (SecurityException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Leave Request ID must be a valid number.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error approving leave.");
        }
    }

    @FXML
    public void rejectLeave() {
        try {
            ensureAuthorized();
            int requestId = parseRequestId();

            boolean exists = pendingLeaveTable.getItems().stream().anyMatch(req -> req.getId() == requestId);
            if (!exists) {
                showAlert(Alert.AlertType.ERROR, "Leave Request ID not found.");
                return;
            }

            LeaveDAO.updateLeaveStatus(requestId, "Rejected");
            logAction("Rejected leave request ID " + requestId);
            showAlert(Alert.AlertType.INFORMATION, "Leave rejected.");
            requestIdField.clear();
            loadPendingLeaves();
        } catch (SecurityException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Leave Request ID must be a valid number.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error rejecting leave.");
        }
    }

    @FXML
    public void loadAttendanceRecords() {
        try {
            ensureAuthorized();
            attendanceTable.setItems(FXCollections.observableArrayList(TimeLogDAO.getAllLogs()));
            userIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            clockInCol.setCellValueFactory(new PropertyValueFactory<>("clockIn"));
            clockOutCol.setCellValueFactory(new PropertyValueFactory<>("clockOut"));
            workingHoursCol.setCellValueFactory(new PropertyValueFactory<>("workingHours"));
            logAction("Loaded all employee attendance records");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to load attendance records.");
        }
    }

    @FXML
    public void loadPendingLeaves() {
        try {
            ensureAuthorized();
            pendingLeaveTable.setItems(FXCollections.observableArrayList(LeaveDAO.getPendingRequests()));
            leaveIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            leaveUserIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
            leaveNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
            endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
            reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
            logAction("Loaded pending leave requests");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to load leave requests.");
        }
    }

    @FXML
    public void handlePasswordReset() {
        try {
            ensureAuthorized();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/reset_password.fxml"));
            Parent root = loader.load();
            ResetPasswordController resetController = loader.getController();
            resetController.setUser(currentAdmin);
            Stage stage = new Stage();
            stage.setTitle("Reset Password");
            stage.setScene(new Scene(root));
            stage.show();
            logAction("Opened password reset window");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to open password reset window.");
        }
    }

    @FXML
    public void handleLogout() {
        try {
            logAction("Logged out");
            Stage currentStage = (Stage) attendanceTable.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/resources/fxml/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(root, 400, 300));
            loginStage.show();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Logout failed.");
        }
    }

    private int parseRequestId() {
        String input = requestIdField.getText() == null ? "" : requestIdField.getText().trim();
        if (input.isEmpty() || !input.matches("\\d+")) {
            throw new NumberFormatException("Invalid ID");
        }
        return Integer.parseInt(input);
    }

    private void clearCreateUserForm() {
        if (createNameField != null) createNameField.clear();
        if (createUsernameField != null) createUsernameField.clear();
        if (createPasswordField != null) createPasswordField.clear();
        if (createRoleCombo != null) createRoleCombo.setValue("employee");
    }

    private void ensureAuthorized() {
        if (currentAdmin == null) {
            throw new SecurityException("No authenticated admin session.");
        }
        SecurityUtils.requireAdmin(currentAdmin.getRole());
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logAction(String action) {
        try {
            if (currentAdmin != null) {
                ActivityLogDAO.log(currentAdmin.getId(), currentAdmin.getUsername(), currentAdmin.getRole(), action);
            }
        } catch (Exception e) {
            System.err.println("Failed to log activity: " + e.getMessage());
        }
    }
}
