package model;

public final class User {
    private int id;
    private String name;
    private String username;
    private String role;
    private int leaveBalance;

    public User(int id, String name, String username, String role) {
        this(id, name, username, role, 0);
    }

    public User(int id, String name, String username, String role, int leaveBalance) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.role = role;
        this.leaveBalance = leaveBalance;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public int getLeaveBalance() {
        return leaveBalance;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", leaveBalance=" + leaveBalance +
                '}';
    }
}
