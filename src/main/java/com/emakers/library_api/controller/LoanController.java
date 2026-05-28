package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.dto.response.LoanResponseDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.LoanRepository;
import com.emakers.library_api.repositores.PersonRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Loans", description = "Endpoints para gerenciamento de empréstimos e devoluções")
public class LoanController {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PersonRepository personRepository;

    @Operation(summary = "Realiza o empréstimo de um livro", description = "Cria um novo registro de empréstimo" +
            " vinculando uma pessoa e um livro utilizando o CPF e o título do livro.")
    @PostMapping("/loans")
    public ResponseEntity<Object> bookLoan (@RequestBody LoanRecordDto loanRecordDto) {

        Optional<BookModel> bookModel = bookRepository.findByTitle(loanRecordDto.title());
        Optional<PersonModel> personModel = personRepository.findByCpf(loanRecordDto.cpf());
        if (bookModel.isEmpty() || personModel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("One or more information is invalid");
        }
        var loanModel = new LoanModel(personModel.get(), bookModel.get());
        var loanResponse = new LoanResponseDto(loanRepository.save(loanModel));
        return ResponseEntity.status(HttpStatus.CREATED).body(loanResponse);
    }

    @Operation(summary = "Realiza a devolução de um livro", description = "Encerra um empréstimo ativo alterando seu" +
            " status para inativo (soft delete) com base no ID do empréstimo.")
    @DeleteMapping("/loans/{id}")
    public ResponseEntity<String> deleteLoan(@PathVariable(value = "id") UUID id) {
        Optional<LoanModel> loanModel = loanRepository.findByIdAndActiveTrue(id);
        if (loanModel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Loan Not Found");
        }
        var newLoan = loanModel.get();
        newLoan.deleteThisLoan();
        loanRepository.save(newLoan);
        return ResponseEntity.status(HttpStatus.OK).body("Delete successful");
    }
}
