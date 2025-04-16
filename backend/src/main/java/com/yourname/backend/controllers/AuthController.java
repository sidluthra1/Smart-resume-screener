package com.yourname.backend.controllers;

import com.yourname.backend.entities.User;
import com.yourname.backend.exceptions.DuplicateUserException;
import com.yourname.backend.services.AuthService;
import com.yourname.backend.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    // simple DTO for both signup and login
    public static class AuthRequest {
        public String email;
        public String password;
    }

    public static class AuthResponse {
        public String token;
        public AuthResponse(String token) { this.token = token; }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest req) {
        try {
            User u = authService.registerUser(req.email, req.password, "HR");
            return ResponseEntity.ok("User created with ID: " + u.getId());
        } catch (DuplicateUserException e) {
            // now you’ll see a 400 and the exception message
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        User user = authService.findUserByEmail(req.email);
        if (user == null || !authService.checkPassword(user, req.password)) {
            return ResponseEntity
                    .status(401)
                    .body("Error: Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
