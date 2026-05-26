package com.emakers.library_api.controller;

import com.emakers.library_api.models.LoginRecordDto;
import com.emakers.library_api.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "Endpoints para autenticação e geração de tokens JWT")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/authenticate")
    @Operation(summary = "Realiza o login do usuário", description = "Recebe as credenciais (e-mail e senha)" +
            " e retorna um token JWT para acesso às rotas protegidas da API.")
    public ResponseEntity<String> authenticate(@RequestBody LoginRecordDto loginRecordDto) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(loginRecordDto.email(), loginRecordDto.password());
        Authentication auth = this.authenticationManager.authenticate(usernamePassword);
        String token = authenticationService.authenticate(auth);
        return ResponseEntity.ok(token);
    }
}
