package com.emakers.library_api.dto.request;

import com.emakers.library_api.validation.ValidCPF;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PersonRecordDto(@NotBlank String name, @ValidCPF @NotBlank String cpf, @NotBlank String zipCode,
                              @Email @NotBlank String email, @NotBlank String password) {
}
