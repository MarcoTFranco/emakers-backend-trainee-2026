package com.emakers.library_api.repositores;

import com.emakers.library_api.models.PersonModel;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PersonRepository extends JpaRepository<PersonModel, UUID> {
    PersonModel findByCpf(String cpf);
}
