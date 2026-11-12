package com.capstone.bookstore.user;

import com.capstone.bookstore.common.BusinessRuleException;
import com.capstone.bookstore.common.NotFoundException;
import com.capstone.bookstore.security.AuthUser;
import com.capstone.bookstore.security.JwtService;
import com.capstone.bookstore.user.UserDtos.AuthResponse;
import com.capstone.bookstore.user.UserDtos.LoginRequest;
import com.capstone.bookstore.user.UserDtos.RegisterRequest;
import com.capstone.bookstore.user.UserDtos.UpdateProfileRequest;
import com.capstone.bookstore.user.UserDtos.UserResponse;
import java.time.Clock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authManager,
                       JwtService jwt, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw BusinessRuleException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName().trim());
        u.setPhone(req.phone());
        u.setRole(Role.CUSTOMER);
        u.setCreatedAt(clock.instant());
        users.save(u);
        return tokenFor(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException (-> 401) on a wrong email or password.
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        User u = users.findByEmailIgnoreCase(req.email().trim()).orElseThrow();
        return tokenFor(u);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(load(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User u = load(userId);
        u.setFullName(req.fullName().trim());
        u.setPhone(req.phone());
        return UserResponse.from(u);
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    private AuthResponse tokenFor(User u) {
        String token = jwt.issue(new AuthUser(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRole()));
        return new AuthResponse(token, "Bearer", jwt.ttlSeconds(), UserResponse.from(u));
    }
}
