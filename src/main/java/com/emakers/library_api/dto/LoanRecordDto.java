package com.emakers.library_api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoanRecordDto(@NotBlank String title, @NotBlank String cpf) {
}
