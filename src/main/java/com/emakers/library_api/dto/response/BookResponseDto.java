package com.emakers.library_api.dto.response;

import com.emakers.library_api.models.BookModel;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.UUID;

public record BookResponseDto(
        UUID id,
        String title,
        String author,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate publicationDate
) {
    public BookResponseDto(BookModel model) {
        this(
                model.getId(),
                model.getTitle(),
                model.getAuthor(),
                model.getPublicationDate()
        );
    }
}