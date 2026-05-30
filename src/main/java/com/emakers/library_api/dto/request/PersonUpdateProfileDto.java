package com.emakers.library_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PersonUpdateProfileDto(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Zip code is required")
        String zipCode,

        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email
) {}
