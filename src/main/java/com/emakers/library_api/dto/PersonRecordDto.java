package com.emakers.library_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

public record PersonRecordDto(@NotBlank String name, @CPF @NotBlank String cpf, @NotBlank String zipCode,
                              @Email @NotBlank String email, @NotBlank String password) {
}
