package com.capstone.bookstore.config;

import com.capstone.bookstore.user.Role;
import com.capstone.bookstore.user.User;
import com.capstone.bookstore.user.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates a demo admin and customer on startup (passwords are hashed here, never stored in SQL). */
@Component
public class SeedDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public SeedDataInitializer(AppProperties props, UserRepository users, PasswordEncoder encoder, Clock clock) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seed().enabled()) {
            return;
        }
        createIfMissing(props.seed().adminEmail(), props.seed().adminPassword(), "Store Admin", Role.ADMIN, 0);
        createIfMissing(props.seed().customerEmail(), props.seed().customerPassword(), "Demo Reader",
                Role.CUSTOMER, 200);
    }

    private void createIfMissing(String email, String password, String name, Role role, int points) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(name);
        u.setRole(role);
        u.setGiftPoints(points);
        u.setCreatedAt(clock.instant());
        users.save(u);
        log.info("Seeded {} user {}", role, email);
    }
}
