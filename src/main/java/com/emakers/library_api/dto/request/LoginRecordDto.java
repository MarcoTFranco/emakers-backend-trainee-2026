package com.emakers.library_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRecordDto(@NotBlank String email,@NotBlank String password) {
}
