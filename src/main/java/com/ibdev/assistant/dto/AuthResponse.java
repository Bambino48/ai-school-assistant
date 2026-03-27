package com.ibdev.assistant.dto;

public class AuthResponse {

    private String message;
    private String status;
    private String name;

    public AuthResponse(String message, String status, String name) {
        this.message = message;
        this.status = status;
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }
}