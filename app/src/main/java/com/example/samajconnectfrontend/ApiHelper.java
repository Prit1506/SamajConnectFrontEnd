package com.example.samajconnectfrontend;

import java.util.HashMap;
import java.util.Map;

public class ApiHelper {

    // Update this with your actual backend URL
    private static final String BASE_URL = "http://10.0.2.2:8080/api/"; // For Android Emulator
    // private static final String BASE_URL = "http://YOUR_IP_ADDRESS:8080/api/"; // For physical device
    // private static final String BASE_URL = "https://your-domain.com/api/"; // For production

    /**
     * Get the base URL for API calls
     * @return Base URL string
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * Get basic headers for API requests
     * @return Map of headers
     */
    public static Map<String, String> getBasicHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    /**
     * Get headers with authorization token
     * @param token JWT token
     * @return Map of headers with authorization
     */
    public static Map<String, String> getAuthHeaders(String token) {
        Map<String, String> headers = getBasicHeaders();
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

    /**
     * Check if the response is successful based on status code
     * @param statusCode HTTP status code
     * @return true if successful, false otherwise
     */
    public static boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Get user-friendly error message based on status code
     * @param statusCode HTTP status code
     * @return Error message string
     */
    public static String getErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Bad request. Please check your input.";
            case 401:
                return "Unauthorized. Please login again.";
            case 403:
                return "Access forbidden.";
            case 404:
                return "Resource not found.";
            case 409:
                return "Conflict. Resource already exists.";
            case 422:
                return "Invalid data provided.";
            case 500:
                return "Server error. Please try again later.";
            case 503:
                return "Service unavailable. Please try again later.";
            default:
                return "An unexpected error occurred.";
        }
    }
}