package com.emakers.library_api.controller;

import com.emakers.library_api.dto.PersonRecordDto;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.repositores.PersonRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Users", description = "Endpoints para gerenciamento de usuários (Pessoas)")
public class PersonController {

    @Autowired
    private PersonRepository personRepository;

    @Operation(summary = "Cadastra um novo usuário", description = "Salva as informações de uma nova" +
            " pessoa no banco de dados.")
    @PostMapping("/user")
    public ResponseEntity<PersonModel> savePerson(@RequestBody @Valid PersonRecordDto personRecordDto) {
        var personModel = new PersonModel(personRecordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(personRepository.save(personModel));
    }

    @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista contendo todas" +
            " as pessoas cadastradas.")
    @GetMapping("/user")
    public ResponseEntity<List<PersonModel>> getAllPersons() {
        return ResponseEntity.status(HttpStatus.OK).body(personRepository.findAll());
    }

    @Operation(summary = "Busca um usuário pelo ID", description = "Retorna os detalhes de uma pessoa" +
            " específica utilizando o seu UUID.")
    @GetMapping("/user/{id}")
    public ResponseEntity<Object> getPersonById(@PathVariable(value = "id") UUID id) {
        Optional<PersonModel> personModel = personRepository.findById(id);
        if (personModel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(personModel.get());
    }

    @Operation(summary = "Atualiza um usuário", description = "Atualiza as informações de uma pessoa" +
            " existente com base no ID fornecido.")
    @PutMapping("/user/{id}")
    public ResponseEntity<Object> updatePerson(@PathVariable(value = "id") UUID id,
                                               @RequestBody @Valid PersonRecordDto personRecordDto) {
        Optional<PersonModel> personModel = personRepository.findById(id);
        if (personModel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        PersonModel personUpdate = personModel.get();
        personUpdate.updatePerson(personRecordDto);
        return ResponseEntity.status(HttpStatus.OK).body(personRepository.save(personUpdate));
    }

    @Operation(summary = "Deleta um usuário", description = "Remove permanentemente uma pessoa do banco" +
            " de dados pelo seu ID.")
    @DeleteMapping("/user/{id}")
    public ResponseEntity<Object> deletePerson(@PathVariable(value = "id") UUID id) {
        Optional<PersonModel> personModel = personRepository.findById(id);
        if (personModel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        personRepository.delete(personModel.get());
        return ResponseEntity.status(HttpStatus.OK).body("Person Deleted");
    }

}
