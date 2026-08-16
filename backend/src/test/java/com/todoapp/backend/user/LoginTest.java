package com.todoapp.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.todoapp.backend.config.SecurityBeans;
import com.todoapp.backend.config.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, SecurityBeans.class })
class LoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private RegistrationService registrationService;

    @BeforeEach
    void storeAlice() {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setPassword(passwordEncoder.encode("password123"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    private MockHttpSession loginAs(String username, String password, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                .param("username", username)
                .param("password", password))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void loginWithCorrectPasswordReturns200() throws Exception {
        loginAs("alice", "password123", 200);
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        loginAs("alice", "wrong-password", 401);
    }

    @Test
    void loginWithUnknownUsernameReturns401() throws Exception {
        loginAs("nobody", "password123", 401);
    }

    @Test
    void loginEstablishesASessionUsableOnProtectedEndpoints() throws Exception {
        MockHttpSession session = loginAs("alice", "password123", 200);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void failedLoginEstablishesNoUsableSession() throws Exception {
        MockHttpSession session = loginAs("alice", "wrong-password", 401);
        if (session != null) {
            mockMvc.perform(get("/api/me").session(session))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void logoutClearsTheSession() throws Exception {
        MockHttpSession session = loginAs("alice", "password123", 200);

        mockMvc.perform(post("/api/logout").session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
