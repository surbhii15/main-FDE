package com.capstone.bookstore.user;

import com.capstone.bookstore.security.AuthUser;
import com.capstone.bookstore.user.UserDtos.AuthResponse;
import com.capstone.bookstore.user.UserDtos.LoginRequest;
import com.capstone.bookstore.user.UserDtos.RegisterRequest;
import com.capstone.bookstore.user.UserDtos.UpdateProfileRequest;
import com.capstone.bookstore.user.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth & Profile", description = "Login page / user authentication (journey steps 1-2)")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a customer account and receive a JWT")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/api/auth/login")
    @SecurityRequirements
    @Operation(summary = "Log in with email and password and receive a JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/api/users/me")
    @Operation(summary = "Current user's profile, including gift-point balance")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return auth.me(user.id());
    }

    @PutMapping("/api/users/me")
    @Operation(summary = "Update the current user's name and phone")
    public UserResponse update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest req) {
        return auth.updateProfile(user.id(), req);
    }
}
