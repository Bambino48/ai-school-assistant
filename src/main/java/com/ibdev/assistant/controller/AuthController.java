package com.ibdev.assistant.controller;

import com.ibdev.assistant.dto.AuthResponse;
import com.ibdev.assistant.dto.LoginRequest;
import com.ibdev.assistant.dto.RegisterRequest;
import com.ibdev.assistant.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpSession session) {
        return authService.login(request, session);
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpSession session) {
        return authService.logout(session);
    }
}