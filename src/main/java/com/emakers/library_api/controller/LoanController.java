package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.dto.response.LoanResponseDto;
import com.emakers.library_api.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @Operation(summary = "Realiza o empréstimo de um livro", description = "Cria um novo empréstimo vinculando pessoa e livro pelos seus UUIDs.")
    @PostMapping
    public ResponseEntity<Object> bookLoan(@RequestBody @Valid LoanRecordDto loanRecordDto, Authentication authentication) {
        Optional<LoanResponseDto> result = loanService.bookLoan(loanRecordDto, authentication);
        return result.<ResponseEntity<Object>>map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book or Person not found"));
    }

    @Operation(summary = "Realiza a devolução de um livro", description = "Encerra um empréstimo ativo (soft delete) pelo ID do empréstimo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteLoan(@PathVariable UUID id, Authentication authentication) {
        if (!loanService.deleteLoan(id, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Loan Not Found");
        }
        return ResponseEntity.ok("Return successful");
    }
}
