package com.emakers.library_api.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.public.key}")
    private RSAPublicKey key;
    @Value("${jwt.private.key}")
    private RSAPrivateKey priv;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth -> auth
                                // Rotas públicas
                                .requestMatchers("/authenticate").permitAll()
                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                                .requestMatchers(HttpMethod.GET, "/books/**").permitAll()

                                // Livros — escrita é admin
                                .requestMatchers(HttpMethod.POST, "/books").hasAuthority("SCOPE_ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/books/**").hasAuthority("SCOPE_ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/books/**").hasAuthority("SCOPE_ADMIN")

                                // Usuários — rotas self-service ANTES dos wildcards admin
                                .requestMatchers(HttpMethod.PUT, "/users/{id}/change-password").authenticated()
                                .requestMatchers(HttpMethod.GET, "/users/{id}").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/users/{id}").authenticated()

                                // Usuários — rotas admin (wildcard cobre o restante)
                                .requestMatchers(HttpMethod.POST, "/users/admin").hasAuthority("SCOPE_ADMIN")
                                .requestMatchers(HttpMethod.GET, "/users/**").hasAuthority("SCOPE_ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/users/**").hasAuthority("SCOPE_ADMIN")

                                // Empréstimos — usuário autenticado pode criar e devolver o próprio
                                .requestMatchers(HttpMethod.POST, "/loans").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/loans/{id}").authenticated()

                                // Empréstimos — demais operações (listar todos, etc.) são admin
                                .requestMatchers("/loans/**").hasAuthority("SCOPE_ADMIN")

                                .anyRequest().authenticated())
                .oauth2ResourceServer(
                        conf -> conf.jwt(
                                jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.key).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.key).privateKey(this.priv).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}
