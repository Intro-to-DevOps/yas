package com.yas.cart;

public class SecurityTest {
    // 1. Fake AWS Key to trigger Gitleaks (Pattern: AWS Access Key ID)
    public static final String AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    public static final String AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    // 2. Hardcoded password to trigger SonarCloud (Security Hotspot / Vulnerability)
    public void connectToDatabase() {
        String dbPassword = "super_secret_password_123!";
        System.out.println("Connecting using password: " + dbPassword);
    }
}
