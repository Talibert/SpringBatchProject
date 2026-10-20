package com.example.api_docker.infra.controller.auth;

import com.example.api_docker.application.auth.result.LoginResult;
import com.example.api_docker.application.auth.usecase.LoginUseCase;
import com.example.api_docker.infra.controller.auth.request.LoginRequest;
import com.example.api_docker.infra.controller.auth.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Endpoints de autenticação e emissão de tokens JWT")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    @Operation(summary = "Autenticar usuário", description = "Valida credenciais (email e senha) e retorna um token JWT de autenticação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou não autorizado")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = loginUseCase.execute(request.toCommand());
        return ResponseEntity.ok(LoginResponse.from(result));
    }
}
