package com.digitalagriculture.api.config;

import com.digitalagriculture.api.user.AccountRole;
import com.digitalagriculture.api.user.User;
import com.digitalagriculture.api.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;

    public BootstrapAdmin(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email}") String email,
            @Value("${app.bootstrap-admin.password}") String password,
            @Value("${app.bootstrap-admin.first-name}") String firstName,
            @Value("${app.bootstrap-admin.last-name}") String lastName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) && !StringUtils.hasText(password)) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) || password.length() < 12) {
            throw new IllegalStateException("Set both BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD (at least 12 characters).");
        }
        if (!users.existsByEmailIgnoreCase(email)) {
            users.save(new User(firstName, lastName, email, null, passwordEncoder.encode(password), AccountRole.ADMIN));
        }
    }
}
