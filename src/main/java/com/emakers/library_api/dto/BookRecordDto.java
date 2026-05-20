package com.emakers.library_api.dto;

import jakarta.validation.constraints.NotBlank;

public record BookRecordDto(@NotBlank String title, @NotBlank String author, @NotBlank String publicationDate) {
}

