package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.PersonPasswordChangeDto;
import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.request.PersonUpdateProfileDto;
import com.emakers.library_api.dto.response.PersonResponseDto;
import com.emakers.library_api.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints para gerenciamento de usuários (Pessoas)")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @Operation(summary = "Cadastra um novo Administrador", description = "Rota exclusiva para admins criarem outros usuários com privilégios de administrador.")
    @PostMapping("/admin")
    public ResponseEntity<PersonResponseDto> saveAdmin(@RequestBody @Valid PersonRecordDto personRecordDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.saveAdmin(personRecordDto));
    }

    @Operation(summary = "Cadastra um novo usuário", description = "Salva as informações de uma nova pessoa no banco de dados.")
    @PostMapping
    public ResponseEntity<PersonResponseDto> savePerson(@RequestBody @Valid PersonRecordDto personRecordDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.savePerson(personRecordDto));
    }

    @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista contendo todas as pessoas cadastradas.")
    @GetMapping
    public ResponseEntity<List<PersonResponseDto>> getAllPersons() {
        return ResponseEntity.ok(personService.getAllPersons());
    }

    @Operation(summary = "Busca um usuário pelo ID", description = "Retorna os detalhes de uma pessoa específica utilizando o seu UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<Object> getPersonById(@PathVariable UUID id) {
        Optional<PersonResponseDto> person = personService.getPersonById(id);
        return person.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found"));
    }

    @Operation(summary = "Atualiza o perfil de um usuário", description = "Atualiza nome, email e CEP. CPF e senha não são alterados aqui.")
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateProfile(@PathVariable UUID id,
                                                @RequestBody @Valid PersonUpdateProfileDto updateDto,
                                                Authentication authentication) {
        Optional<PersonResponseDto> updated = personService.updateProfile(id, updateDto, authentication);
        return updated.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found"));
    }

    @Operation(summary = "Altera a senha de um usuário", description = "Permite que um usuário autenticado altere sua própria senha.")
    @PutMapping("/{id}/change-password")
    public ResponseEntity<Object> changePassword(@PathVariable UUID id,
                                                 @RequestBody @Valid PersonPasswordChangeDto changeDto,
                                                 Authentication authentication) {
        Optional<PersonResponseDto> result = personService.changePassword(id, changeDto, authentication);
        return result.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found"));
    }

    @Operation(summary = "Deleta um usuário", description = "Remove permanentemente uma pessoa do banco de dados pelo seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletePerson(@PathVariable UUID id) {
        if (!personService.deletePerson(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        return ResponseEntity.ok("Person Deleted");
    }
}
