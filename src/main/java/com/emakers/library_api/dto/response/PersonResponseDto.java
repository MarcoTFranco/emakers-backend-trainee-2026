package com.emakers.library_api.dto.response;

import com.emakers.library_api.models.PersonModel;

import java.util.UUID;

public record PersonResponseDto(
        UUID id,
        String name,
        String email
) {
    public PersonResponseDto(PersonModel model) {
        this(model.getId(), model.getName(), model.getEmail());
    }
}
