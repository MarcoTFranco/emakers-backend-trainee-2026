package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.response.PersonResponseDto;
import com.emakers.library_api.dto.response.ViaCepResponseDto;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import com.emakers.library_api.repositores.PersonRepository;
import com.emakers.library_api.service.PersonService;
import com.emakers.library_api.service.ViaCepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Operation(summary = "Cadastra um novo Administrador", description = "Rota exclusiva para admins criarem" +
            " outros usuários com privilégios de administrador.")
    @PostMapping("/admin")
    public ResponseEntity<PersonResponseDto> saveAdmin(@RequestBody @Valid PersonRecordDto personRecordDto) {
        var personResponseDto = personService.saveAdmin(personRecordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(personResponseDto);
    }

    @Operation(summary = "Cadastra um novo usuário", description = "Salva as informações de uma nova" +
            " pessoa no banco de dados.")
    @PostMapping()
    public ResponseEntity<Object> savePerson(@RequestBody @Valid PersonRecordDto personRecordDto) {
        try {
            PersonResponseDto personResponseDto = personService.savePerson(personRecordDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(personResponseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista contendo todas" +
            " as pessoas cadastradas.")
    @GetMapping()
    public ResponseEntity<List<PersonResponseDto>> getAllPersons() {
        return ResponseEntity.status(HttpStatus.OK).body(personService.getAllPersons());
    }

    @Operation(summary = "Busca um usuário pelo ID", description = "Retorna os detalhes de uma pessoa" +
            " específica utilizando o seu UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<Object> getPersonById(@PathVariable UUID id) {
        Optional<PersonResponseDto> personResponseDto = personService.getPersonById(id);
        return personResponseDto.<ResponseEntity<Object>>map(responseDto -> ResponseEntity
                .status(HttpStatus.OK).body(responseDto)).orElseGet(() -> ResponseEntity
                .status(HttpStatus.NOT_FOUND).body("Person Not Found"));
    }

    @Operation(summary = "Atualiza um usuário", description = "Atualiza as informações de uma pessoa" +
            " existente com base no ID fornecido.")
    @PutMapping("/{id}")
    public ResponseEntity<Object> updatePerson(@PathVariable UUID id,
                                               @RequestBody @Valid PersonRecordDto personRecordDto,
                                               Authentication authentication) {
        try {
            Optional<PersonResponseDto> updatedPerson = personService.updatePerson(id, personRecordDto, authentication);
            return updatedPerson.<ResponseEntity<Object>>map(personResponseDto -> ResponseEntity
                    .status(HttpStatus.OK).body(personResponseDto)).orElseGet(() -> ResponseEntity
                    .status(HttpStatus.NOT_FOUND).body("Person Not Found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @Operation(summary = "Deleta um usuário", description = "Remove permanentemente uma pessoa do banco" +
            " de dados pelo seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletePerson(@PathVariable UUID id) {
        boolean deleted = personService.deletePerson(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        return ResponseEntity.status(HttpStatus.OK).body("Person Deleted");
    }
}
