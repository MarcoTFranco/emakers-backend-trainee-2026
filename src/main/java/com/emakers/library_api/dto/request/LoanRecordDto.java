package com.emakers.library_api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LoanRecordDto(@NotNull UUID bookId, @NotNull UUID personId) {
}
