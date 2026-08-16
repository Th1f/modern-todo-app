package com.todoapp.backend.user;

import com.todoapp.backend.category.CategoryDefaults;
import com.todoapp.backend.error.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CategoryDefaults categoryDefaults;

    public RegistrationService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            CategoryDefaults categoryDefaults) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.categoryDefaults = categoryDefaults;
    }

    @Transactional
    public User register(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw new ConflictException("\"" + username + "\" is taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        users.save(user);

        categoryDefaults.createFor(user);
        return user;
    }
}
