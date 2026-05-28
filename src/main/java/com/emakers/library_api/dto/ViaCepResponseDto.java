package com.emakers.library_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ViaCepResponseDto(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        Boolean erro
) {
}
