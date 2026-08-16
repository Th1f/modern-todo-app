package com.todoapp.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.todoapp.backend.config.SecurityBeans;
import com.todoapp.backend.config.SecurityConfig;
import com.todoapp.backend.error.ApiExceptionHandler;
import com.todoapp.backend.error.ConflictException;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, SecurityBeans.class, ApiExceptionHandler.class })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private UserRepository userRepository;

    // ---------- register ----------

    @Test
    void registerIsReachableWithoutLoggingIn() throws Exception {
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        verify(registrationService).register("alice", "password123");
    }

    @Test
    void registerConflictBecomes409() throws Exception {
        when(registrationService.register("alice", "password123"))
                .thenThrow(new ConflictException("\"alice\" is taken"));

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsShortPasswordBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any(), any());
    }

    @Test
    void registerRejectsBlankUsernameBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"   \",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any(), any());
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
