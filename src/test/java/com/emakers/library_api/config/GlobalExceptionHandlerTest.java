package com.emakers.library_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleSecurity_returns403() {
        ResponseEntity<String> resp = handler.handleSecurity(new SecurityException("denied"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isEqualTo("denied");
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<String> resp = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isEqualTo("bad input");
    }
}
