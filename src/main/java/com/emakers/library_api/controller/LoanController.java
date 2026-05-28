package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.dto.response.LoanResponseDto;
import com.emakers.library_api.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/loans")
@Tag(name = "Loans", description = "Endpoints para gerenciamento de empréstimos e devoluções")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(summary = "Realiza o empréstimo de um livro", description = "Cria um novo registro de empréstimo" +
            " vinculando uma pessoa e um livro utilizando o CPF e o título do livro.")
    @PostMapping()
    public ResponseEntity<Object> bookLoan (@RequestBody LoanRecordDto loanRecordDto) {
        Optional<LoanResponseDto> loanResponseDto = loanService.bookLoan(loanRecordDto);
        return loanResponseDto.<ResponseEntity<Object>>map(responseDto -> ResponseEntity
                .status(HttpStatus.CREATED).body(responseDto)).orElseGet(() -> ResponseEntity
                .status(HttpStatus.NOT_FOUND).body("One or more information is invalid"));
    }

    @Operation(summary = "Realiza a devolução de um livro", description = "Encerra um empréstimo ativo alterando seu" +
            " status para inativo (soft delete) com base no ID do empréstimo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLoan(@PathVariable UUID id) {
        boolean status = loanService.deleteLoan(id);
        if (!status) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Loan Not Found");
        }
        return ResponseEntity.status(HttpStatus.OK).body("Delete successful");
    }
}
