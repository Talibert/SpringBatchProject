package com.example.api_docker;

import com.example.api_docker.domain.shared.EventType;
import com.example.api_docker.domain.user.*;
import com.example.api_docker.domain.user.event.UserCreatedEvent;
import com.example.api_docker.domain.user.event.UserPasswordChangedEvent;
import com.example.api_docker.infra.controller.user.request.ChangePasswordRequest;
import com.example.api_docker.infra.controller.user.request.CreateUserRequest;
import com.example.api_docker.infra.kafka.user.UserEventsConsumer;
import com.example.api_docker.infra.security.JwtTokenGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserEventsE2EIntegrationTest extends IntegrationAbstractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private UserEventsConsumer userEventsConsumer;

    @Test
    @DisplayName("E2E: Deve cadastrar usuário via HTTP, salvar no banco e disparar evento consumido pelo Kafka")
    void shouldRegisterUserViaHttpAndConsumeEvent() throws Exception {
        String adminToken = jwtTokenGenerator.generate(
                UserId.generate(),
                new Email("admin@test.com"),
                UserRole.ADMIN
        );

        CreateUserRequest request = new CreateUserRequest(
                "Maria",
                "Souza",
                "maria.souza@teste.com",
                "senha123"
        );

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        Optional<User> userOptional = userRepository.findByEmail(new Email("maria.souza@teste.com"));
        assertTrue(userOptional.isPresent());

        ArgumentCaptor<UserCreatedEvent> captor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(userEventsConsumer, timeout(10000).atLeastOnce()).onUserCreated(captor.capture());

        UserCreatedEvent consumedEvent = captor.getAllValues().stream()
                .filter(e -> "maria.souza@teste.com".equals(e.email().value()))
                .findFirst()
                .orElseThrow();

        assertNotNull(consumedEvent.userId());
        assertEquals("maria.souza@teste.com", consumedEvent.email().value());
        assertEquals(EventType.USER_CREATED_EVENT, consumedEvent.eventType());
    }

    @Test
    @DisplayName("E2E: Deve alterar senha via HTTP, atualizar no banco e disparar evento consumido pelo Kafka")
    void shouldChangePasswordViaHttpAndConsumeEvent() throws Exception {
        User seedUser = userRepository.findByEmail(new Email("admin@test.com")).orElseThrow();

        String userToken = jwtTokenGenerator.generate(
                seedUser.getId(),
                seedUser.getEmail(),
                UserRole.ADMIN
        );

        ChangePasswordRequest request = new ChangePasswordRequest(
                "senha-test-123",
                "nova-senha-super-segura"
        );

        mockMvc.perform(patch("/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        ArgumentCaptor<UserPasswordChangedEvent> captor = ArgumentCaptor.forClass(UserPasswordChangedEvent.class);
        verify(userEventsConsumer, timeout(10000).times(1)).onUserPasswordChanged(captor.capture());

        UserPasswordChangedEvent consumedEvent = captor.getValue();
        assertEquals(seedUser.getId(), consumedEvent.userId());
        assertEquals(EventType.USER_PASSWORD_CHANGED_EVENT, consumedEvent.eventType());
    }
}
