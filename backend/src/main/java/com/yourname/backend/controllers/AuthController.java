package com.yourname.backend.controllers;

import com.yourname.backend.entities.User;
import com.yourname.backend.services.AuthService;
import com.yourname.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public String signup(@RequestParam("email") String email, @RequestParam("password") String password) {
        try {
            User newUser = authService.registerUser(email, password, "HR");
            return "User created with ID: " + newUser.getId();
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam("email") String email, @RequestParam("password") String password) {
        User user = authService.findUserByEmail(email);
        if (user == null) {
            return "Error: User not found";
        }

        boolean valid = authService.checkPassword(user, password);
        if (!valid) {
            return "Error: Invalid credentials";
        }

        String token = jwtUtil.generateToken(email);
        return "JWT Token: " + token;
    }
}
