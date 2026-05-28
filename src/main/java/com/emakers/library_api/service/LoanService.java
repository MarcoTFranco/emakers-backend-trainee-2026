package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.dto.response.LoanResponseDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.LoanRepository;
import com.emakers.library_api.repositores.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;

    private final BookRepository bookRepository;

    private final PersonRepository personRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, PersonRepository personRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.personRepository = personRepository;
    }

    public Optional<LoanResponseDto> bookLoan (LoanRecordDto loanRecordDto) {
        Optional<BookModel> bookModel = bookRepository.findByTitle(loanRecordDto.title());
        Optional<PersonModel> personModel = personRepository.findByCpf(loanRecordDto.cpf());
        if (bookModel.isEmpty() || personModel.isEmpty()) {
            return Optional.empty();
        }
        var loanModel = new LoanModel(personModel.get(), bookModel.get());
        var loanResponse = new LoanResponseDto(loanRepository.save(loanModel));
        return Optional.of(loanResponse);
    }

    public Boolean deleteLoan(UUID id) {
        Optional<LoanModel> loanModel = loanRepository.findByIdAndActiveTrue(id);
        if (loanModel.isEmpty()) {
            return false;
        }
        var newLoan = loanModel.get();
        newLoan.deleteThisLoan();
        loanRepository.save(newLoan);
        return true;
    }


}
