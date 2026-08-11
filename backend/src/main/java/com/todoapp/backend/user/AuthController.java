package com.todoapp.backend.user;

import com.todoapp.backend.category.CategoryDefaults;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryDefaults categoryDefaults;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CategoryDefaults categoryDefaults) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryDefaults = categoryDefaults;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        if(userRepository.existsByUsername(request.username())){
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        categoryDefaults.createFor(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String,String>> me(Authentication auth){
        return ResponseEntity.ok(Map.of("username",auth.getName()));
    }

}
