package com.emakers.library_api.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookRecordDto(
        @NotBlank
        String title,
        @NotBlank
        String author,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate publicationDate) {
}

