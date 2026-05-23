package com.emakers.library_api.controller;

import com.emakers.library_api.dto.LoanRecordDto;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.LoanRepository;
import com.emakers.library_api.repositores.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
public class LoanController {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PersonRepository personRepository;

    @PostMapping("/loan")
    public ResponseEntity<Object> bookLoan (@RequestBody LoanRecordDto loanRecordDto) {
        var bookModel = bookRepository.findByTitle(loanRecordDto.title());
        var personModel = personRepository.findByCpf(loanRecordDto.cpf());
        if (bookModel == null || personModel == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("One or more information is invalid");
        }
        var loanModel = new LoanModel(personModel, bookModel);
        return ResponseEntity.status(HttpStatus.OK).body(loanRepository.save(loanModel));
    }

    @DeleteMapping("/loan/{id}")
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
