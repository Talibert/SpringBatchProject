package com.example.api_docker.infra.controller.auth;

import com.example.api_docker.ControllerAbstractTests;
import com.example.api_docker.application.auth.result.LoginResult;
import com.example.api_docker.application.auth.usecase.LoginUseCase;
import com.example.api_docker.domain.user.exception.InvalidCredentialsException;
import com.example.api_docker.infra.controller.auth.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends ControllerAbstractTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @Test
    void shouldReturnTokenWhenLoginWithValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("joao@email.com", "senha123");
        LoginResult loginResult = new LoginResult(
                "jwt-token-gerado",
                UUID.randomUUID(),
                "João Silva",
                "STUDENT"
        );

        when(loginUseCase.execute(any())).thenReturn(loginResult);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-gerado"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void shouldReturn401WhenLoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("joao@email.com", "senha-errada");

        doThrow(new InvalidCredentialsException())
                .when(loginUseCase).execute(any());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}