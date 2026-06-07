package com.Springboot.Chatbot.service.Impl;

import com.Springboot.Chatbot.dto.AuthResponse;
import com.Springboot.Chatbot.dto.LoginRequest;
import com.Springboot.Chatbot.dto.RegisterRequest;
import com.Springboot.Chatbot.entity.User;
import com.Springboot.Chatbot.error.InvalidCredentialsException;
import com.Springboot.Chatbot.error.UserAlreadyExistsException;
import com.Springboot.Chatbot.repository.UserRepository;
import com.Springboot.Chatbot.security.JwtUtil;
import com.Springboot.Chatbot.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    @Override
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + request.email()
            );
        }

        // Create and save new user
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        // Generate token and return response
        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token, user.getName(), user.getEmail());

    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate token and return response
        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}
