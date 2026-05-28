package com.emakers.library_api.controller;

import com.emakers.library_api.dto.PersonRecordDto;
import com.emakers.library_api.dto.ViaCepResponseDto;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import com.emakers.library_api.repositores.PersonRepository;
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
@Tag(name = "Users", description = "Endpoints para gerenciamento de usuários (Pessoas)")
public class PersonController {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ViaCepService viaCepService;

    @Operation(summary = "Cadastra um novo Administrador", description = "Rota exclusiva para admins criarem" +
            " outros usuários com privilégios de administrador.")
    @PostMapping("/users/admin")
    public ResponseEntity<PersonModel> saveAdmin(@RequestBody @Valid PersonRecordDto personRecordDto) {
        var personModel = new PersonModel(personRecordDto);
        personModel.setRole(UserRole.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(personRepository.save(personModel));
    }

    @Operation(summary = "Cadastra um novo usuário", description = "Salva as informações de uma nova" +
            " pessoa no banco de dados.")
    @PostMapping("/users")
    public ResponseEntity<Object> savePerson(@RequestBody @Valid PersonRecordDto personRecordDto) {
        try {
            ViaCepResponseDto zipCode = viaCepService.checkCep(personRecordDto.zipCode());
            if (zipCode == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The provided zip code does not exist.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        var personModel = new PersonModel(personRecordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(personRepository.save(personModel));
    }

    @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista contendo todas" +
            " as pessoas cadastradas.")
    @GetMapping("/users")
    public ResponseEntity<List<PersonModel>> getAllPersons() {
        return ResponseEntity.status(HttpStatus.OK).body(personRepository.findAll());
    }

    @Operation(summary = "Busca um usuário pelo ID", description = "Retorna os detalhes de uma pessoa" +
            " específica utilizando o seu UUID.")
    @GetMapping("/users/{id}")
    public ResponseEntity<Object> getPersonById(@PathVariable(value = "id") UUID id) {
        Optional<PersonModel> personModel = personRepository.findById(id);
        if (personModel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(personModel.get());
    }

    @Operation(summary = "Atualiza um usuário", description = "Atualiza as informações de uma pessoa" +
            " existente com base no ID fornecido.")
    @PutMapping("/users/{id}")
    public ResponseEntity<Object> updatePerson(@PathVariable(value = "id") UUID id,
                                               @RequestBody @Valid PersonRecordDto personRecordDto,
                                               Authentication authentication) {
        Optional<PersonModel> personModelOptional = personRepository.findById(id);
        if (personModelOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        PersonModel personDoBanco = personModelOptional.get();
        String usuarioLogadoEmail = authentication.getName();
        boolean isNotOwner = !usuarioLogadoEmail.equals(personDoBanco.getEmail());
        boolean isNotAdmin = authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"));
        if (isNotOwner && isNotAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only change your own data!");
        }
        personDoBanco.updatePerson(personRecordDto);
        return ResponseEntity.status(HttpStatus.OK).body(personRepository.save(personDoBanco));
    }

    @Operation(summary = "Deleta um usuário", description = "Remove permanentemente uma pessoa do banco" +
            " de dados pelo seu ID.")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Object> deletePerson(@PathVariable(value = "id") UUID id) {
        Optional<PersonModel> personModel = personRepository.findById(id);
        if (personModel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        personRepository.delete(personModel.get());
        return ResponseEntity.status(HttpStatus.OK).body("Person Deleted");
    }

}
