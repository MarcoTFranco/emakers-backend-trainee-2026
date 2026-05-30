package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.dto.response.LoanResponseDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.LoanRepository;
import com.emakers.library_api.repositores.PersonRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Transactional
    public Optional<LoanResponseDto> bookLoan(LoanRecordDto loanRecordDto, Authentication authentication) {
        Optional<BookModel> bookModel = bookRepository.findById(loanRecordDto.bookId());
        if (bookModel.isEmpty()) {
            return Optional.empty();
        }

        Optional<PersonModel> personModel = personRepository.findById(loanRecordDto.personId());
        if (personModel.isEmpty()) {
            return Optional.empty();
        }

        UUID authenticatedUserId = getAuthenticatedUserId(authentication);
        if (!authenticatedUserId.equals(loanRecordDto.personId())) {
            if (!isAdmin(authentication)) {
                throw new SecurityException("You can only create loans for yourself");
            }
        }

        if (loanRepository.existsByPersonAndBookAndActiveTrue(personModel.get(), bookModel.get())) {
            throw new IllegalArgumentException("This book is already on loan by this user.");
        }

        long activeLoans = loanRepository.countByPersonAndActiveTrue(personModel.get());
        if (activeLoans >= 5) {
            throw new IllegalArgumentException("User has reached maximum loan limit (5 books)");
        }

        var loanModel = new LoanModel(personModel.get(), bookModel.get());
        return Optional.of(new LoanResponseDto(loanRepository.save(loanModel)));
    }

    public List<LoanResponseDto> getAllActiveLoan() {
        return loanRepository.findByActiveTrue().stream()
                .map(LoanResponseDto::new)
                .toList();
    }

    @Transactional
    public Boolean deleteLoan(UUID id, Authentication authentication) {
        Optional<LoanModel> loanModel = loanRepository.findByIdAndActiveTrue(id);
        if (loanModel.isEmpty()) {
            return false;
        }

        LoanModel loan = loanModel.get();
        UUID authenticatedUserId = getAuthenticatedUserId(authentication);
        boolean isOwner = authenticatedUserId.equals(loan.getPerson().getId());

        if (!isOwner && !isAdmin(authentication)) {
            throw new SecurityException("You can only return your own loans");
        }

        loan.deleteThisLoan();
        loanRepository.save(loan);
        return true;
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"));
    }
}
