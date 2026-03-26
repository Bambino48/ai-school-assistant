package com.ibdev.assistant.dto;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record ChatStreamSession(Long conversationId, StreamingResponseBody stream) {
}