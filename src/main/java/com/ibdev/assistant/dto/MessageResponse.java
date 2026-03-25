package com.ibdev.assistant.dto;

import lombok.Getter;

@Getter
public class MessageResponse {

    private String message;
    private String status;

    public MessageResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }

}