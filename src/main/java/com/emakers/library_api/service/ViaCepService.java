package com.emakers.library_api.service;

import com.emakers.library_api.dto.ViaCepResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ViaCepService {

    public ViaCepResponseDto checkCep(String cep){

        String cleanCep = cep.replaceAll("[^0-9]", "");

        if (cleanCep.length() != 8){
            throw new IllegalArgumentException("Invalid CEP. Must be 8 characters long.");
        }

        String url = "https://viacep.com.br/ws/" + cleanCep + "/json/";

        RestTemplate restTemplate = new RestTemplate();

        try {
            ViaCepResponseDto response = restTemplate.getForObject(url, ViaCepResponseDto.class);
            if (response != null && Boolean.TRUE.equals(response.erro())) {
                return null;
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error when querying ViaCEP", e);
        }
    }

}
