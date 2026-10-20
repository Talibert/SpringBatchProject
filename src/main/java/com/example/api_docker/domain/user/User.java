package com.example.api_docker.domain.user;

import com.example.api_docker.domain.shared.DomainEvent;
import com.example.api_docker.domain.shared.exception.DomainException;
import com.example.api_docker.domain.user.event.UserCreatedEvent;
import com.example.api_docker.domain.user.event.UserPasswordChangedEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class User {

    private final UserId id;
    private final FullName name;
    private final Email email;
    private String passwordHash;
    private final LocalDateTime createdAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private User(UserId id, FullName name, Email email,
                   String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank())
            throw new DomainException("Hash da senha não pode ser vazio");

        this.passwordHash = newPasswordHash;
        domainEvents.add(onPasswordChanged());
    }

    public static User create(FullName name, Email email, String passwordHash) {
        User user = new User(UserId.generate(), name, email, passwordHash, LocalDateTime.now());
        user.addDomainEvent(new UserCreatedEvent(user.getId(), email));
        return user;
    }

    public static User restore(UserId id, FullName name, Email email,
                                String passwordHash, LocalDateTime createdAt) {
        return new User(id, name, email, passwordHash, createdAt);
    }

    protected DomainEvent onPasswordChanged() {
        return new UserPasswordChangedEvent(getId());
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    protected void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }
}
