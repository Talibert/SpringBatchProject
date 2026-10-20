package com.example.api_docker.application.user.usecase;

import com.example.api_docker.UnitAbstractTests;
import com.example.api_docker.application.user.query.ListUsersQuery;
import com.example.api_docker.application.user.result.UserResult;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;
import com.example.api_docker.domain.user.Email;
import com.example.api_docker.domain.user.FullName;
import com.example.api_docker.domain.user.User;
import com.example.api_docker.domain.user.UserId;
import com.example.api_docker.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest extends UnitAbstractTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListUsersUseCase listUsersUseCase;

    @Test
    @DisplayName("Deve executar busca paginada e mapear para UserResult")
    void shouldExecutePaginatedSearchAndMapToUserResult() {
        PaginationRequest pagination = PaginationRequest.of(0, 10, "email", "ASC");
        ListUsersQuery query = new ListUsersQuery(pagination);

        User user = User.restore(
                UserId.generate(),
                new FullName("John", "Doe"),
                new Email("john.doe@example.com"),
                "hashed-pass",
                LocalDateTime.now()
        );

        PageResult<User> domainPage = new PageResult<>(
                List.of(user),
                0,
                10,
                1L,
                1
        );

        when(userRepository.findAll(pagination)).thenReturn(domainPage);

        PageResult<UserResult> result = listUsersUseCase.execute(query);

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals("John Doe", result.items().getFirst().fullName());
        assertEquals("john.doe@example.com", result.items().getFirst().email());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());

        verify(userRepository, times(1)).findAll(pagination);
    }
}
