package com.emakers.library_api.repositores;

import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<PersonModel, UUID> {
    Optional<PersonModel> findByCpf(String cpf);
    Optional<PersonModel> findByEmail(String email);
    Optional<PersonModel> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByCpf(String cpf);
    boolean existsByRole(UserRole role);
}
