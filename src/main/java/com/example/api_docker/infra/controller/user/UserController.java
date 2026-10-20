package com.example.api_docker.infra.controller.user;

import com.example.api_docker.application.user.query.GetUserQuery;
import com.example.api_docker.application.user.query.ListUsersQuery;
import com.example.api_docker.application.user.result.UserResult;
import com.example.api_docker.application.user.usecase.ChangeUserPasswordUseCase;
import com.example.api_docker.application.user.usecase.CreateUserUseCase;
import com.example.api_docker.application.user.usecase.GetUserUseCase;
import com.example.api_docker.application.user.usecase.ListUsersUseCase;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;
import com.example.api_docker.domain.user.UserId;
import com.example.api_docker.infra.controller.response.PageResponse;
import com.example.api_docker.infra.controller.user.request.ChangePasswordRequest;
import com.example.api_docker.infra.controller.user.request.CreateUserRequest;
import com.example.api_docker.infra.controller.user.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Usuários", description = "Endpoints de gerenciamento e consulta de usuários")
@SecurityRequirement(name = "bearerAuth")
@RestController
@AllArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final ChangeUserPasswordUseCase changeUserPasswordUseCase;

    @Operation(summary = "Listar usuários com paginação", description = "Retorna uma página de usuários com metadados de paginação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> findAll(
            @Parameter(description = "Número da página (iniciando em 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de registros por página", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo para ordenação", example = "createdAt")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Direção da ordenação (ASC ou DESC)", example = "DESC")
            @RequestParam(defaultValue = "ASC") String sortDirection
    ) {
        PaginationRequest pagination = PaginationRequest.of(page, size, sortBy, sortDirection);
        PageResult<UserResult> result = listUsersUseCase.execute(new ListUsersQuery(pagination));
        return ResponseEntity.ok(PageResponse.from(result, UserResponse::from));
    }

    @Operation(summary = "Cadastrar usuário", description = "Cadastra um novo usuário no sistema e publica o evento UserCreatedEvent no Kafka")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid CreateUserRequest request) {
        createUserUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Obter dados do usuário autenticado", description = "Retorna os dados cadastrais do usuário associado ao token JWT atual")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados do usuário retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserId userId) {
        UserResult result = getUserUseCase.execute(new GetUserQuery(userId));
        return ResponseEntity.ok(UserResponse.from(result));
    }

    @Operation(summary = "Buscar usuário por ID", description = "Busca os detalhes cadastrais de um usuário pelo seu identificador UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        UserResult result = getUserUseCase.execute(new GetUserQuery(new UserId(id)));
        return ResponseEntity.ok(UserResponse.from(result));
    }

    @Operation(summary = "Alterar senha do usuário autenticado", description = "Valida a senha atual, salva o novo hash criptografado e publica evento no Kafka")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou nova senha inválida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserId userId,
                                               @RequestBody @Valid ChangePasswordRequest request) {
        changeUserPasswordUseCase.execute(request.toCommand(userId));
        return ResponseEntity.noContent().build();
    }
}
