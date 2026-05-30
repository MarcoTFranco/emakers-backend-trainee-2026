package com.emakers.library_api.repositores;

import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<LoanModel, UUID> {
    Optional<LoanModel> findByIdAndActiveTrue(UUID id);
    List<LoanModel> findByActiveTrue();
    List<LoanModel> findByPersonAndActiveTrue(PersonModel person);
    long countByPersonAndActiveTrue(PersonModel person);
    boolean existsByPersonAndBookAndActiveTrue(PersonModel person, BookModel book);
}
