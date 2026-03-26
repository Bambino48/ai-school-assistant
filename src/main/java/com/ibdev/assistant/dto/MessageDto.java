package com.ibdev.assistant.dto;

public class MessageDto {

    private String content;
    private String role;

    public MessageDto(String content, String role) {
        this.content = content;
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public String getRole() {
        return role;
    }
}