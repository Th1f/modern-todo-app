package com.todoapp.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.todoapp.backend.category.CategoryDefaults;
import com.todoapp.backend.config.SecurityBeans;
import com.todoapp.backend.config.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, SecurityBeans.class })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private CategoryDefaults categoryDefaults;

    // ---------- register ----------

    @Test
    void registerIsReachableWithoutLoggingIn() throws Exception {
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void registerSeedsDefaultCategories() throws Exception {
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<User> seeded = ArgumentCaptor.forClass(User.class);
        verify(categoryDefaults).createFor(seeded.capture());
        assertThat(seeded.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    void registerHashesThePassword() throws Exception {
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        String stored = saved.getValue().getPassword();
        assertThat(stored).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", stored)).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());

        verify(userRepository, never()).save(any(User.class));
        verify(categoryDefaults, never()).createFor(any(User.class));
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"   \",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- me ----------

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser("alice")
    void meReturnsTheLoggedInUsername() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @WithMockUser("john")
    void meReflectsWhoeverIsLoggedIn() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }
}
