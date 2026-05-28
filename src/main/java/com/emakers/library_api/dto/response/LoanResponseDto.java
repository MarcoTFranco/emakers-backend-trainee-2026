package com.emakers.library_api.dto.response;

import com.emakers.library_api.models.LoanModel;

import java.util.UUID;

public record LoanResponseDto(
        UUID loanId,
        String personName,
        String bookTitle,
        Boolean active
) {
    public LoanResponseDto(LoanModel model) {
        this(model.getId(), model.getPerson().getName(), model.getBook().getTitle(), model.getActive());
    }
}