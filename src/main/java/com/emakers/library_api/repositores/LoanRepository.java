package com.emakers.library_api.repositores;

import com.emakers.library_api.models.LoanModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<LoanModel, UUID> {
    Optional<LoanModel> findByIdAndActiveTrue(UUID id);
}
