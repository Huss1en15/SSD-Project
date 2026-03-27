package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public final class DBConnection {
    private static final String PROPERTIES_FILE = "config.properties";
    private static final Properties properties;

    static {
        properties = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new RuntimeException("Could not find " + PROPERTIES_FILE + " in classpath");
            }
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties file: " + e.getMessage(), e);
        }
    }

    private DBConnection() {}

    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = readSetting("db.url", "DB_URL");
        String user = readSetting("db.user", "DB_USER");
        String password = readSetting("db.password", "DB_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }

    private static String readSetting(String propertyKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(propertyKey);
    }
}
