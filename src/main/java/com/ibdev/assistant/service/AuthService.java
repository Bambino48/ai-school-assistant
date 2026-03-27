package com.ibdev.assistant.service;

import com.ibdev.assistant.dto.AuthResponse;
import com.ibdev.assistant.dto.LoginRequest;
import com.ibdev.assistant.dto.RegisterRequest;
import com.ibdev.assistant.entity.User;
import com.ibdev.assistant.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new AuthResponse("Email déjà utilisé", "error", null);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return new AuthResponse("Inscription réussie", "success", user.getName());
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return new AuthResponse("Utilisateur introuvable", "error", null);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse("Mot de passe incorrect", "error", null);
        }

        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());

        return new AuthResponse("Connexion réussie", "success", user.getName());
    }

    public AuthResponse logout(HttpSession session) {
        session.invalidate();
        return new AuthResponse("Déconnexion réussie", "success", null);
    }
}