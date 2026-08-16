package com.todoapp.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.todoapp.backend.category.CategoryDefaults;
import com.todoapp.backend.error.ConflictException;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryDefaults categoryDefaults;

    // The real encoder, so registerHashesThePassword() asserts something.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private RegistrationService service() {
        return new RegistrationService(userRepository, passwordEncoder, categoryDefaults);
    }

    @Test
    void registerStoresTheUserAndSeedsCategories() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        User created = service().register("alice", "password123");

        assertThat(created.getUsername()).isEqualTo("alice");
        verify(userRepository).save(created);

        ArgumentCaptor<User> seeded = ArgumentCaptor.forClass(User.class);
        verify(categoryDefaults).createFor(seeded.capture());
        assertThat(seeded.getValue()).isSameAs(created);
    }

    @Test
    void registerHashesThePassword() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        User created = service().register("alice", "password123");

        assertThat(created.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", created.getPassword())).isTrue();
    }

    @Test
    void registerRejectsATakenUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service().register("alice", "password123"))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(categoryDefaults, never()).createFor(any(User.class));
    }
}
