package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.LoanRepository;
import com.emakers.library_api.repositores.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock BookRepository bookRepository;
    @Mock PersonRepository personRepository;
    @InjectMocks LoanService loanService;

    private UUID bookId, personId, loanId;
    private BookModel book;
    private PersonModel person;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        personId = UUID.randomUUID();
        loanId = UUID.randomUUID();
        book = mock(BookModel.class);
        person = mock(PersonModel.class);
        lenient().when(person.getId()).thenReturn(personId);
    }

    private Authentication mockAuthWithUserId(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("userId")).thenReturn(userId.toString());
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(jwt);
        lenient().when(auth.getAuthorities()).thenReturn(Collections.emptyList());
        return auth;
    }

    @Test
    void bookLoan_returnsEmptyWhenBookNotFound() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        Authentication auth = mockAuthWithUserId(personId);

        var result = loanService.bookLoan(new LoanRecordDto(bookId, personId), auth);

        assertThat(result).isEmpty();
    }

    @Test
    void bookLoan_returnsEmptyWhenPersonNotFound() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(personRepository.findById(personId)).thenReturn(Optional.empty());
        Authentication auth = mockAuthWithUserId(personId);

        var result = loanService.bookLoan(new LoanRecordDto(bookId, personId), auth);

        assertThat(result).isEmpty();
    }

    @Test
    void bookLoan_throwsSecurityWhenNotOwnerNotAdmin() {
        Authentication authAsOther = mockAuthWithUserId(UUID.randomUUID());
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> loanService.bookLoan(new LoanRecordDto(bookId, personId), authAsOther))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void bookLoan_throwsWhenDuplicateLoan() {
        Authentication auth = mockAuthWithUserId(personId);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(loanRepository.existsByPersonAndBookAndActiveTrue(person, book)).thenReturn(true);

        assertThatThrownBy(() -> loanService.bookLoan(new LoanRecordDto(bookId, personId), auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already on loan");
    }

    @Test
    void bookLoan_throwsWhenLimitExceeded() {
        Authentication auth = mockAuthWithUserId(personId);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(loanRepository.existsByPersonAndBookAndActiveTrue(person, book)).thenReturn(false);
        when(loanRepository.countByPersonAndActiveTrue(person)).thenReturn(5L);

        assertThatThrownBy(() -> loanService.bookLoan(new LoanRecordDto(bookId, personId), auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum loan limit");
    }

    @Test
    void deleteLoan_returnsFalseWhenNotFound() {
        Authentication auth = mockAuthWithUserId(personId);
        when(loanRepository.findByIdAndActiveTrue(loanId)).thenReturn(Optional.empty());

        assertThat(loanService.deleteLoan(loanId, auth)).isFalse();
    }

    @Test
    void deleteLoan_throwsSecurityWhenNotOwnerNotAdmin() {
        Authentication authAsOther = mockAuthWithUserId(UUID.randomUUID());
        LoanModel loan = mock(LoanModel.class);
        when(loan.getPerson()).thenReturn(person);
        when(loanRepository.findByIdAndActiveTrue(loanId)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.deleteLoan(loanId, authAsOther))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("own loans");
    }
}
