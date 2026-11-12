package com.capstone.bookstore.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password,
            @NotBlank @Size(max = 150) String fullName,
            @Pattern(regexp = "^[+0-9 -]{7,20}$", message = "must be a valid phone number") String phone) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}

    public record UserResponse(Long id, String email, String fullName, String phone, Role role,
                               int giftPoints, Instant createdAt) {

        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                    u.getGiftPoints(), u.getCreatedAt());
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 150) String fullName,
            @Pattern(regexp = "^[+0-9 -]{7,20}$", message = "must be a valid phone number") String phone) {}
}
