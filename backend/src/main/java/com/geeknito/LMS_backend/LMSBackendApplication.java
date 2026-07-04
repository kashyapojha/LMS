package com.geeknito.LMS_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LMSBackendApplication {

    public static void main(String[] args) {
        try {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                java.util.Map<String, String> envVars = new java.util.HashMap<>();
                java.nio.file.Files.lines(envFile.toPath())
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            }
                            System.setProperty(key, value);
                            envVars.put(key, value);
                            // Map UPPER_SNAKE env keys to Spring dot-notation system properties
                            String springKey = toSpringPropertyKey(key);
                            if (springKey != null) {
                                System.setProperty(springKey, value);
                            }
                        }
                    });
                
                // Also explicitly map Cloudinary env-var keys to Spring property keys
                // so that @Value("${cloudinary.cloud-name}") resolves correctly
                if (envVars.containsKey("CLOUDINARY_CLOUD_NAME")) {
                    System.setProperty("cloudinary.cloud-name", envVars.get("CLOUDINARY_CLOUD_NAME"));
                }
                if (envVars.containsKey("CLOUDINARY_API_KEY")) {
                    System.setProperty("cloudinary.api-key", envVars.get("CLOUDINARY_API_KEY"));
                }
                if (envVars.containsKey("CLOUDINARY_API_SECRET")) {
                    System.setProperty("cloudinary.api-secret", envVars.get("CLOUDINARY_API_SECRET"));
                }
                
                System.out.println("[.env loader] Loaded env keys: " + envVars.keySet());
                System.out.println("[.env loader] cloudinary.cloud-name = " + System.getProperty("cloudinary.cloud-name"));
                System.out.println("[.env loader] cloudinary.api-key = " + System.getProperty("cloudinary.api-key"));
            } else {
                System.out.println("[.env loader] No .env file found, relying on environment variables.");
            }
        } catch (Exception e) {
            System.err.println("Failed to parse local .env file: " + e.getMessage());
        }
        SpringApplication.run(LMSBackendApplication.class, args);
    }

    /** Converts SPRING_DATASOURCE_URL-style keys to spring.datasource.url for Spring Boot. */
    private static String toSpringPropertyKey(String envKey) {
        if (!envKey.contains("_")) {
            return null;
        }
        String lower = envKey.toLowerCase().replace('_', '.');
        if (lower.startsWith("spring.") || lower.startsWith("cloudinary.")) {
            return lower;
        }
        return null;
    }
}
