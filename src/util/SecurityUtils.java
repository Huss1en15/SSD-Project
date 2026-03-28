package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class SecurityUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern SAFE_REASON_PATTERN = Pattern.compile("^[a-zA-Z0-9 .,!?()\\-]{5,200}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z ]{2,50}$");

    private SecurityUtils() {}

    public static String hashPassword(String password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt);
        return "PBKDF2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" +
                Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(String password, String storedPassword) throws Exception {
        if (password == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("PBKDF2$")) {
            String[] parts = storedPassword.split("\\$");
            if (parts.length != 4) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        }

        String legacyHash = legacySha256(password);
        return MessageDigest.isEqual(
                legacyHash.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static boolean needsRehash(String storedPassword) {
        return storedPassword == null || !storedPassword.startsWith("PBKDF2$");
    }

    public static String legacySha256(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean isStrongPassword(String password) {
        return password != null &&
                password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toLowerCase();
        return normalized.equals("admin") || normalized.equals("employee") || normalized.equals("supervisor")
                || normalized.equals("manager") || normalized.equals("hr") || normalized.equals("hr_manager");
    }

    public static boolean isValidLeaveReason(String reason) {
        return reason != null && SAFE_REASON_PATTERN.matcher(reason).matches();
    }

    public static String sanitizeText(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[<>\"';]", "").trim();
    }

    public static void requireAdmin(String role) {
        if (role == null || !"admin".equalsIgnoreCase(role)) {
            throw new SecurityException("Access denied. Admin role required.");
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) throws Exception {
        return pbkdf2(password, salt, ITERATIONS);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) throws Exception {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Secure password hashing algorithm unavailable.", e);
        }
    }
}
