package de.aivot.GoverBackend.services;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class PasswordService {
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    public static boolean testPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
