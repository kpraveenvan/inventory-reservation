package com.praveen.www.dto;

import java.util.Map;

/**
 * Standard error payload returned by the API.
 */
public class ErrorResponse {

    private final String code;
    private final String message;
    private final Map<String, Object> details;

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    public ErrorResponse(String code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
