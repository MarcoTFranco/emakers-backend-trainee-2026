package com.emakers.library_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoanRecordDto(@NotBlank String title, @NotBlank String cpf) {
}
