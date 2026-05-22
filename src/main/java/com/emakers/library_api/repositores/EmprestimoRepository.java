package com.emakers.library_api.repositores;

import com.emakers.library_api.models.PersonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmprestimoRepository extends JpaRepository<PersonModel, UUID> {
}
