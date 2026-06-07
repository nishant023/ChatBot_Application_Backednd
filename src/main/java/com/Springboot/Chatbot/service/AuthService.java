package com.Springboot.Chatbot.service;

import com.Springboot.Chatbot.dto.AuthResponse;
import com.Springboot.Chatbot.dto.LoginRequest;
import com.Springboot.Chatbot.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
