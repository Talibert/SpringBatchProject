package com.example.api_docker.infra.config;

import com.example.api_docker.application.user.command.CreateUserCommand;
import com.example.api_docker.application.user.usecase.CreateUserUseCase;
import com.example.api_docker.domain.user.Email;
import com.example.api_docker.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "admin.seed.enabled", havingValue = "true", matchIfMissing = true)
public class UserSeedConfig {

    @Bean
    public CommandLineRunner seedUser(
            CreateUserUseCase createUserUseCase,
            UserRepository userRepository,
            @Value("${admin.seed.firstName}") String firstName,
            @Value("${admin.seed.lastName}") String lastName,
            @Value("${admin.seed.email}") String email,
            @Value("${admin.seed.password}") String password) {
        return args -> {
            Email userEmail = new Email(email);
            if (!userRepository.existsByEmail(userEmail)) {
                createUserUseCase.execute(new CreateUserCommand(firstName, lastName, email, password));

                log.info("User inicial criado: {}", email);
            } else {
                log.warn("User inicial já existe, seed ignorado.");
            }
        };
    }
}
