# Fix Security & Bugs — Library API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir 10 bugs críticos identificados em code review: regras de segurança invertidas, validações ausentes, trava de empréstimo duplicado, JWT estável após troca de email, status HTTP incorretos e conflito Flyway/Hibernate.

**Architecture:** Os bugs se dividem em 3 camadas — configuração de segurança (SecurityConfig), lógica de negócio (Services) e contrato de API (DTOs/Controllers). Cada task é independente e pode ser commitada separadamente. A Strategy para stale-JWT: adicionar `userId` (UUID imutável) como claim no token, trocando comparações por email por comparações por UUID.

**Tech Stack:** Spring Boot 4.0.6, Spring Security 6 (OAuth2 Resource Server/JWT), Flyway, JPA/Hibernate, PostgreSQL, JUnit 5, Mockito.

---

## Mapa de Arquivos

| Arquivo | Ação | Motivo |
|---|---|---|
| `application.properties` | Modificar | ddl-auto=update → validate (conflito Flyway) |
| `config/GlobalExceptionHandler.java` | **Criar** | @RestControllerAdvice centralizado |
| `models/PersonAuthenticated.java` | Modificar | Expor getId() para JwtService |
| `service/JwtService.java` | Modificar | Adicionar claim `userId` ao JWT |
| `config/SecurityConfig.java` | Modificar | Corrigir ordem das regras (wildcard antes da específica) |
| `dto/request/LoanRecordDto.java` | Modificar | title+cpf → bookId+personId (UUID) |
| `repositores/LoanRepository.java` | Modificar | Adicionar existsByPersonAndBookAndActiveTrue |
| `service/PersonService.java` | Modificar | saveAdmin validation + ViaCEP no update + UUID ownership |
| `service/LoanService.java` | Modificar | UUID lookup + duplicate guard + @Transactional + ownership deleteLoan |
| `controller/LoanController.java` | Modificar | @Valid, Authentication no deleteLoan, remover try-catch |
| `controller/PersonController.java` | Modificar | Remover imports mortos, remover try-catch |

---

## Task 1: `application.properties` — Corrigir conflito Flyway/Hibernate

**Problema:** `ddl-auto=update` + Flyway habilitado = dois gerenciadores de schema. Hibernate pode tentar reverter migrations.

**Files:**
- Modify: `src/main/resources/application.properties:5`

- [ ] **Step 1: Alterar ddl-auto para validate**

Em `src/main/resources/application.properties`, trocar a linha:
```properties
spring.jpa.hibernate.ddl-auto=update
```
por:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

- [ ] **Step 2: Verificar que a aplicação sobe sem erro**

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/library_api"
```
Esperado: nenhum `SchemaManagementException` ou `HibernateException` ao iniciar.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.properties
git commit -m "fix: usa ddl-auto=validate para nao conflitar com Flyway"
```

---

## Task 2: `GlobalExceptionHandler` — Centralizador de erros HTTP

**Problema:** Cada controller tem try-catch duplicado para `SecurityException` → 403 e `IllegalArgumentException` → 400. Um @RestControllerAdvice centraliza e elimina duplicação.

**Files:**
- Create: `src/main/java/com/emakers/library_api/config/GlobalExceptionHandler.java`

- [ ] **Step 1: Criar o handler**

```java
package com.emakers.library_api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurity(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
```

- [ ] **Step 2: Testar unitariamente**

Criar `src/test/java/com/emakers/library_api/config/GlobalExceptionHandlerTest.java`:

```java
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
```

- [ ] **Step 3: Rodar o teste**

```bash
mvn test -pl . -Dtest=GlobalExceptionHandlerTest -q
```
Esperado: `BUILD SUCCESS`, 2 testes passando.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/emakers/library_api/config/GlobalExceptionHandler.java
git add src/test/java/com/emakers/library_api/config/GlobalExceptionHandlerTest.java
git commit -m "feat: adiciona GlobalExceptionHandler centralizado com @RestControllerAdvice"
```

---

## Task 3: JWT com `userId` — Corrigir stale-JWT após troca de email

**Problema:** Após `updateProfile` trocar o email, o JWT existente tem o email antigo como `sub`. As verificações de ownership que comparam `authentication.getName()` (email antigo) com `personModel.getEmail()` (email novo) falham → usuário recebe 403 nas próprias rotas.

**Solução:** Adicionar o UUID imutável da pessoa como claim `userId` no JWT. As verificações de ownership passam a comparar UUIDs em vez de emails.

**Files:**
- Modify: `src/main/java/com/emakers/library_api/models/PersonAuthenticated.java`
- Modify: `src/main/java/com/emakers/library_api/service/JwtService.java`

- [ ] **Step 1: Expor getId() em PersonAuthenticated**

Em `src/main/java/com/emakers/library_api/models/PersonAuthenticated.java`, adicionar após o construtor:

```java
import java.util.UUID;
```

E o método (inserir após o construtor `PersonAuthenticated(PersonModel personModel)`):

```java
public UUID getId() {
    return personModel.getId();
}
```

O arquivo completo após a mudança:
```java
package com.emakers.library_api.models;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PersonAuthenticated implements UserDetails {

    private final PersonModel personModel;

    public PersonAuthenticated(PersonModel personModel) {
        this.personModel = personModel;
    }

    public UUID getId() {
        return personModel.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (personModel.getRole() == UserRole.ADMIN) {
            return List.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("USER"));
        } else {
            return List.of(new SimpleGrantedAuthority("USER"));
        }
    }

    @Override
    public @Nullable String getPassword() {
        return personModel.getPassword();
    }

    @Override
    public String getUsername() {
        return personModel.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
```

- [ ] **Step 2: Adicionar claim `userId` no JwtService**

Substituir o conteúdo de `src/main/java/com/emakers/library_api/service/JwtService.java` por:

```java
package com.emakers.library_api.service;

import com.emakers.library_api.models.PersonAuthenticated;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtEncoder encoder;

    public JwtService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 3600L;

        PersonAuthenticated user = (PersonAuthenticated) authentication.getPrincipal();

        String scopes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        var claims = JwtClaimsSet.builder()
                .issuer("spring-security-jwt")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(authentication.getName())
                .claim("scope", scopes)
                .claim("userId", user.getId().toString())
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
```

- [ ] **Step 3: Testar unitariamente o JwtService**

Criar `src/test/java/com/emakers/library_api/service/JwtServiceTest.java`:

```java
package com.emakers.library_api.service;

import com.emakers.library_api.models.PersonAuthenticated;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder encoder;

    @InjectMocks
    private JwtService jwtService;

    @Test
    void generateToken_includesUserIdClaim() {
        PersonModel personModel = mock(PersonModel.class);
        when(personModel.getRole()).thenReturn(UserRole.USER);
        when(personModel.getId()).thenReturn(java.util.UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(personModel.getEmail()).thenReturn("user@test.com");
        when(personModel.getPassword()).thenReturn("hash");

        PersonAuthenticated principal = new PersonAuthenticated(personModel);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("USER")));

        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(mock(org.springframework.security.oauth2.jwt.Jwt.class));

        jwtService.generateToken(auth);

        verify(encoder).encode(argThat(params -> {
            var claims = params.getClaims();
            return "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa".equals(claims.getClaims().get("userId").toString());
        }));
    }
}
```

- [ ] **Step 4: Rodar o teste**

```bash
mvn test -Dtest=JwtServiceTest -q
```
Esperado: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/emakers/library_api/models/PersonAuthenticated.java
git add src/main/java/com/emakers/library_api/service/JwtService.java
git add src/test/java/com/emakers/library_api/service/JwtServiceTest.java
git commit -m "feat: inclui userId (UUID) como claim no JWT para ownership check estavel"
```

---

## Task 4: `SecurityConfig` — Corrigir ordem das regras

**Problema:** Spring Security avalia regras na ordem de declaração (primeira que casa vence).
- `GET /users/**` (SCOPE_ADMIN) na linha 51 casa antes de `GET /users/{id}` (authenticated) na linha 55 → usuário comum recebe 403 no próprio perfil.
- `/loans/**` (SCOPE_ADMIN) sem restrição de método bloqueia `POST /loans` e `DELETE /loans/{id}` para todos os não-admins.
- As regras `authenticated()` das linhas 55-56 são dead code.

**Files:**
- Modify: `src/main/java/com/emakers/library_api/config/SecurityConfig.java:38-62`

- [ ] **Step 1: Reordenar as regras no filterChain**

Substituir o bloco `authorizeHttpRequests` pelo seguinte (mais específico antes de mais genérico):

```java
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

                            // Usuários — rotas admin
                            .requestMatchers(HttpMethod.POST, "/users/admin").hasAuthority("SCOPE_ADMIN")
                            .requestMatchers(HttpMethod.GET, "/users/**").hasAuthority("SCOPE_ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/users/**").hasAuthority("SCOPE_ADMIN")

                            // Empréstimos — usuário pode criar e devolver o próprio
                            .requestMatchers(HttpMethod.POST, "/loans").authenticated()
                            .requestMatchers(HttpMethod.DELETE, "/loans/{id}").authenticated()

                            // Empréstimos — demais operações (listar todos) são admin
                            .requestMatchers("/loans/**").hasAuthority("SCOPE_ADMIN")

                            .anyRequest().authenticated())
            .oauth2ResourceServer(
                    conf -> conf.jwt(
                            jwt -> jwt.decoder(jwtDecoder())));
    return http.build();
}
```

- [ ] **Step 2: Verificar que nenhuma regra existente foi perdida**

Conferir mentalmente:
- `POST /users` (registro) → `permitAll()` ✓
- `GET /books` (listar livros) → `permitAll()` via `/books/**` ✓
- `PUT /users/{id}/change-password` → `authenticated()` ✓ (antes do wildcard GET)
- `GET /users/{id}` → `authenticated()` ✓ (antes do `GET /users/**`)
- `PUT /users/{id}` → `authenticated()` ✓
- `POST /loans` → `authenticated()` ✓
- `DELETE /loans/{id}` → `authenticated()` ✓
- `GET /loans` (listar todos) → cai em `/loans/**` → `SCOPE_ADMIN` ✓

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/emakers/library_api/config/SecurityConfig.java
git commit -m "fix: corrige ordem das regras no SecurityConfig (wildcards apos especificos)"
```

---

## Task 5: `LoanRecordDto` e `LoanRepository` — UUID em vez de título/CPF

**Problema:** Identificar livro por título é ambíguo (dois livros com mesmo nome). CPF no body da requisição é PII desnecessário — livro e pessoa devem ser identificados por UUID estável.

**Files:**
- Modify: `src/main/java/com/emakers/library_api/dto/request/LoanRecordDto.java`
- Modify: `src/main/java/com/emakers/library_api/repositores/LoanRepository.java`

- [ ] **Step 1: Atualizar LoanRecordDto para usar UUIDs**

Substituir o conteúdo de `src/main/java/com/emakers/library_api/dto/request/LoanRecordDto.java`:

```java
package com.emakers.library_api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LoanRecordDto(@NotNull UUID bookId, @NotNull UUID personId) {
}
```

- [ ] **Step 2: Adicionar método de trava de duplicata no LoanRepository**

Em `src/main/java/com/emakers/library_api/repositores/LoanRepository.java`, adicionar o método:

```java
boolean existsByPersonAndBookAndActiveTrue(PersonModel person, BookModel book);
```

O arquivo completo após a mudança:

```java
package com.emakers.library_api.repositores;

import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<LoanModel, UUID> {
    Optional<LoanModel> findByIdAndActiveTrue(UUID id);
    List<LoanModel> findByActiveTrue();
    List<LoanModel> findByPersonAndActiveTrue(PersonModel person);
    long countByPersonAndActiveTrue(PersonModel person);
    boolean existsByPersonAndBookAndActiveTrue(PersonModel person, BookModel book);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/emakers/library_api/dto/request/LoanRecordDto.java
git add src/main/java/com/emakers/library_api/repositores/LoanRepository.java
git commit -m "fix: LoanRecordDto usa bookId/personId (UUID) em vez de titulo/CPF para evitar PII e ambiguidade"
```

---

## Task 6: `PersonService` — Corrigir validações e ownership checks

**Problemas corrigidos nesta task:**
1. `saveAdmin` não valida unicidade de email/CPF nem CEP → pode persistir dados corrompidos.
2. `updateProfile` não valida o CEP via ViaCEP → aceita CEPs inválidos.
3. Ownership checks comparam emails (instáveis) → corrigir para UUID via claim JWT.
4. `changePassword` lança `SecurityException` para senha incorreta → HTTP 403 errado; deve ser `IllegalArgumentException` → HTTP 400.

**Files:**
- Modify: `src/main/java/com/emakers/library_api/service/PersonService.java`

- [ ] **Step 1: Escrever testes unitários para PersonService**

Criar `src/test/java/com/emakers/library_api/service/PersonServiceTest.java`:

```java
package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.PersonPasswordChangeDto;
import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.request.PersonUpdateProfileDto;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import com.emakers.library_api.repositores.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock PersonRepository personRepository;
    @Mock ViaCepService viaCepService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks PersonService personService;

    private PersonModel existingPerson;
    private UUID personId;
    private Authentication authAsOwner;

    @BeforeEach
    void setUp() {
        personId = UUID.randomUUID();
        existingPerson = mock(PersonModel.class);
        when(existingPerson.getId()).thenReturn(personId);
        when(existingPerson.getEmail()).thenReturn("user@test.com");
        when(existingPerson.getPassword()).thenReturn("$2a$hash");

        authAsOwner = mockAuthWithUserId(personId);
    }

    // Helper: cria Authentication com userId claim
    private Authentication mockAuthWithUserId(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("userId")).thenReturn(userId.toString());
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        return auth;
    }

    // --- saveAdmin ---

    @Test
    void saveAdmin_throwsWhenEmailDuplicate() {
        when(personRepository.existsByEmailIgnoreCase("admin@test.com")).thenReturn(true);
        PersonRecordDto dto = new PersonRecordDto("Admin", "111.444.777-35", "35500-000", "admin@test.com", "admin123");

        assertThatThrownBy(() -> personService.saveAdmin(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-mail");
    }

    @Test
    void saveAdmin_throwsWhenCpfDuplicate() {
        when(personRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(personRepository.existsByCpf("111.444.777-35")).thenReturn(true);
        PersonRecordDto dto = new PersonRecordDto("Admin", "111.444.777-35", "35500-000", "admin@test.com", "admin123");

        assertThatThrownBy(() -> personService.saveAdmin(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void saveAdmin_throwsWhenInvalidCep() {
        when(personRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(personRepository.existsByCpf(any())).thenReturn(false);
        when(viaCepService.checkCep("00000-000")).thenReturn(null);
        PersonRecordDto dto = new PersonRecordDto("Admin", "111.444.777-35", "00000-000", "admin@test.com", "admin123");

        assertThatThrownBy(() -> personService.saveAdmin(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zip code");
    }

    // --- updateProfile ---

    @Test
    void updateProfile_throwsWhenInvalidCep() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(existingPerson));
        when(viaCepService.checkCep("00000-000")).thenReturn(null);
        PersonUpdateProfileDto dto = new PersonUpdateProfileDto("Name", "00000-000", "user@test.com");

        assertThatThrownBy(() -> personService.updateProfile(personId, dto, authAsOwner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zip code");
    }

    @Test
    void updateProfile_throwsWhenEmailTaken() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(existingPerson));
        when(viaCepService.checkCep(any())).thenReturn(mock(com.emakers.library_api.dto.response.ViaCepResponseDto.class));
        when(existingPerson.getEmail()).thenReturn("user@test.com");
        when(personRepository.existsByEmailIgnoreCase("other@test.com")).thenReturn(true);
        PersonUpdateProfileDto dto = new PersonUpdateProfileDto("Name", "35500-000", "other@test.com");

        assertThatThrownBy(() -> personService.updateProfile(personId, dto, authAsOwner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void updateProfile_throwsWhenNotOwnerNotAdmin() {
        UUID otherId = UUID.randomUUID();
        Authentication authAsOther = mockAuthWithUserId(otherId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(existingPerson));
        PersonUpdateProfileDto dto = new PersonUpdateProfileDto("Name", "35500-000", "user@test.com");

        assertThatThrownBy(() -> personService.updateProfile(personId, dto, authAsOther))
                .isInstanceOf(SecurityException.class);
    }

    // --- changePassword ---

    @Test
    void changePassword_throwsIllegalArgWhenPasswordWrong() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(existingPerson));
        when(passwordEncoder.matches("wrongPass", "$2a$hash")).thenReturn(false);
        PersonPasswordChangeDto dto = new PersonPasswordChangeDto("wrongPass", "newPassword123");

        assertThatThrownBy(() -> personService.changePassword(personId, dto, authAsOwner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void changePassword_throwsSecurityWhenNotOwner() {
        UUID otherId = UUID.randomUUID();
        Authentication authAsOther = mockAuthWithUserId(otherId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(existingPerson));

        assertThatThrownBy(() -> personService.changePassword(personId, new PersonPasswordChangeDto("x", "newpassword1"), authAsOther))
                .isInstanceOf(SecurityException.class);
    }
}
```

- [ ] **Step 2: Rodar os testes (esperar falhar)**

```bash
mvn test -Dtest=PersonServiceTest -q 2>&1 | tail -20
```
Esperado: falhas indicando que `saveAdmin` não valida, `updateProfile` não valida CEP, etc.

- [ ] **Step 3: Implementar as correções em PersonService**

Substituir o conteúdo completo de `src/main/java/com/emakers/library_api/service/PersonService.java`:

```java
package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.PersonPasswordChangeDto;
import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.request.PersonUpdateProfileDto;
import com.emakers.library_api.dto.response.PersonResponseDto;
import com.emakers.library_api.dto.response.ViaCepResponseDto;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import com.emakers.library_api.repositores.PersonRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final ViaCepService viaCepService;
    private final PasswordEncoder passwordEncoder;

    public PersonService(PersonRepository personRepository, ViaCepService viaCepService, PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.viaCepService = viaCepService;
        this.passwordEncoder = passwordEncoder;
    }

    public PersonResponseDto saveAdmin(PersonRecordDto personRecordDto) {
        validatePersonConstraints(personRecordDto.email(), personRecordDto.cpf(), personRecordDto.zipCode());
        var personModel = new PersonModel(personRecordDto, passwordEncoder);
        personModel.setRole(UserRole.ADMIN);
        return new PersonResponseDto(personRepository.save(personModel));
    }

    public PersonResponseDto savePerson(PersonRecordDto personRecordDto) {
        validatePersonConstraints(personRecordDto.email(), personRecordDto.cpf(), personRecordDto.zipCode());
        var personModel = new PersonModel(personRecordDto, passwordEncoder);
        return new PersonResponseDto(personRepository.save(personModel));
    }

    public List<PersonResponseDto> getAllPersons() {
        return personRepository.findAll().stream()
                .map(PersonResponseDto::new)
                .toList();
    }

    public Optional<PersonResponseDto> getPersonById(UUID id) {
        return personRepository.findById(id).map(PersonResponseDto::new);
    }

    public Optional<PersonResponseDto> updateProfile(UUID id, PersonUpdateProfileDto updateDto, Authentication authentication) {
        Optional<PersonModel> personModelOptional = personRepository.findById(id);
        if (personModelOptional.isEmpty()) {
            return Optional.empty();
        }

        PersonModel personModel = personModelOptional.get();
        UUID authenticatedUserId = getAuthenticatedUserId(authentication);
        boolean isOwner = authenticatedUserId.equals(id);
        boolean isAdmin = isAdmin(authentication);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You can only change your own profile!");
        }

        String normalizedEmail = updateDto.email().toLowerCase().trim();
        if (!normalizedEmail.equals(personModel.getEmail()) && personRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        if (viaCepService.checkCep(updateDto.zipCode()) == null) {
            throw new IllegalArgumentException("The provided zip code does not exist.");
        }

        personModel.updateProfile(updateDto.name(), updateDto.zipCode(), normalizedEmail);
        return Optional.of(new PersonResponseDto(personRepository.save(personModel)));
    }

    public Optional<PersonResponseDto> changePassword(UUID id, PersonPasswordChangeDto changeDto, Authentication authentication) {
        Optional<PersonModel> personModelOptional = personRepository.findById(id);
        if (personModelOptional.isEmpty()) {
            return Optional.empty();
        }

        PersonModel personModel = personModelOptional.get();
        UUID authenticatedUserId = getAuthenticatedUserId(authentication);

        if (!authenticatedUserId.equals(id)) {
            throw new SecurityException("You can only change your own password!");
        }

        if (!passwordEncoder.matches(changeDto.currentPassword(), personModel.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        personModel.setPassword(passwordEncoder.encode(changeDto.newPassword()));
        return Optional.of(new PersonResponseDto(personRepository.save(personModel)));
    }

    public boolean deletePerson(UUID id) {
        Optional<PersonModel> personModel = personRepository.findById(id);
        if (personModel.isEmpty()) {
            return false;
        }
        personRepository.delete(personModel.get());
        return true;
    }

    private void validatePersonConstraints(String email, String cpf, String zipCode) {
        String normalizedEmail = email.toLowerCase().trim();
        if (personRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("This E-mail is already registered.");
        }
        if (personRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("This CPF is already registered.");
        }
        if (viaCepService.checkCep(zipCode) == null) {
            throw new IllegalArgumentException("The provided zip code does not exist.");
        }
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"));
    }
}
```

- [ ] **Step 4: Rodar os testes (esperar passar)**

```bash
mvn test -Dtest=PersonServiceTest -q
```
Esperado: `BUILD SUCCESS`, todos os testes passando.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/emakers/library_api/service/PersonService.java
git add src/test/java/com/emakers/library_api/service/PersonServiceTest.java
git commit -m "fix: saveAdmin valida constraints, updateProfile valida CEP, ownership por UUID, changePassword retorna 400 para senha errada"
```

---

## Task 7: `LoanService` — UUID lookup, trava de duplicata, @Transactional e ownership no deleteLoan

**Problemas corrigidos:**
1. `bookLoan` usava `findByTitle`/`findByCpf` → trocar para `findById` com os novos campos UUID do `LoanRecordDto`.
2. Nenhuma trava impede que a mesma pessoa pegue o mesmo livro duas vezes.
3. Ownership check em `bookLoan` comparava emails → trocar para UUID.
4. `deleteLoan` sem `@Transactional` e sem verificação de ownership.

**Files:**
- Modify: `src/main/java/com/emakers/library_api/service/LoanService.java`
- Modify: `src/main/java/com/emakers/library_api/controller/LoanController.java` (Step 7 do Task 8)

- [ ] **Step 1: Escrever testes unitários para LoanService**

Criar `src/test/java/com/emakers/library_api/service/LoanServiceTest.java`:

```java
package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.LoanRepository;
import com.emakers.library_api.repositores.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock BookRepository bookRepository;
    @Mock PersonRepository personRepository;
    @InjectMocks LoanService loanService;

    private UUID bookId, personId, loanId;
    private BookModel book;
    private PersonModel person;
    private Authentication authAsOwner;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        personId = UUID.randomUUID();
        loanId = UUID.randomUUID();

        book = mock(BookModel.class);
        person = mock(PersonModel.class);
        when(person.getId()).thenReturn(personId);

        authAsOwner = mockAuthWithUserId(personId);
    }

    private Authentication mockAuthWithUserId(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("userId")).thenReturn(userId.toString());
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        return auth;
    }

    @Test
    void bookLoan_throwsWhenDuplicateLoan() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(loanRepository.existsByPersonAndBookAndActiveTrue(person, book)).thenReturn(true);
        LoanRecordDto dto = new LoanRecordDto(bookId, personId);

        assertThatThrownBy(() -> loanService.bookLoan(dto, authAsOwner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already on loan");
    }

    @Test
    void bookLoan_throwsWhenLimitExceeded() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(loanRepository.existsByPersonAndBookAndActiveTrue(person, book)).thenReturn(false);
        when(loanRepository.countByPersonAndActiveTrue(person)).thenReturn(5L);
        LoanRecordDto dto = new LoanRecordDto(bookId, personId);

        assertThatThrownBy(() -> loanService.bookLoan(dto, authAsOwner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum loan limit");
    }

    @Test
    void bookLoan_throwsSecurityWhenNotOwnerNotAdmin() {
        UUID otherId = UUID.randomUUID();
        Authentication authAsOther = mockAuthWithUserId(otherId);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        LoanRecordDto dto = new LoanRecordDto(bookId, personId);

        assertThatThrownBy(() -> loanService.bookLoan(dto, authAsOther))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void bookLoan_returnsEmptyWhenBookNotFound() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        LoanRecordDto dto = new LoanRecordDto(bookId, personId);

        var result = loanService.bookLoan(dto, authAsOwner);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteLoan_throwsSecurityWhenNotOwner() {
        UUID otherId = UUID.randomUUID();
        Authentication authAsOther = mockAuthWithUserId(otherId);
        LoanModel loan = mock(LoanModel.class);
        when(loan.getPerson()).thenReturn(person);
        when(loanRepository.findByIdAndActiveTrue(loanId)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.deleteLoan(loanId, authAsOther))
                .isInstanceOf(SecurityException.class);
    }
}
```

- [ ] **Step 2: Rodar testes (esperar falhar)**

```bash
mvn test -Dtest=LoanServiceTest -q 2>&1 | tail -20
```
Esperado: erros de compilação/falhas sobre métodos ausentes.

- [ ] **Step 3: Implementar as correções em LoanService**

Substituir o conteúdo completo de `src/main/java/com/emakers/library_api/service/LoanService.java`:

```java
package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.dto.response.LoanResponseDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.LoanModel;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.LoanRepository;
import com.emakers.library_api.repositores.PersonRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final PersonRepository personRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, PersonRepository personRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.personRepository = personRepository;
    }

    @Transactional
    public Optional<LoanResponseDto> bookLoan(LoanRecordDto loanRecordDto, Authentication authentication) {
        Optional<BookModel> bookModel = bookRepository.findById(loanRecordDto.bookId());
        if (bookModel.isEmpty()) {
            return Optional.empty();
        }

        Optional<PersonModel> personModel = personRepository.findById(loanRecordDto.personId());
        if (personModel.isEmpty()) {
            return Optional.empty();
        }

        UUID authenticatedUserId = getAuthenticatedUserId(authentication);
        if (!authenticatedUserId.equals(loanRecordDto.personId())) {
            boolean isAdmin = isAdmin(authentication);
            if (!isAdmin) {
                throw new SecurityException("You can only create loans for yourself");
            }
        }

        if (loanRepository.existsByPersonAndBookAndActiveTrue(personModel.get(), bookModel.get())) {
            throw new IllegalArgumentException("This book is already on loan by this user.");
        }

        long activeLoans = loanRepository.countByPersonAndActiveTrue(personModel.get());
        if (activeLoans >= 5) {
            throw new IllegalArgumentException("User has reached maximum loan limit (5 books)");
        }

        var loanModel = new LoanModel(personModel.get(), bookModel.get());
        var loanResponse = new LoanResponseDto(loanRepository.save(loanModel));
        return Optional.of(loanResponse);
    }

    public List<LoanResponseDto> getAllActiveLoan() {
        return loanRepository.findByActiveTrue().stream()
                .map(LoanResponseDto::new)
                .toList();
    }

    @Transactional
    public Boolean deleteLoan(UUID id, Authentication authentication) {
        Optional<LoanModel> loanModel = loanRepository.findByIdAndActiveTrue(id);
        if (loanModel.isEmpty()) {
            return false;
        }

        LoanModel loan = loanModel.get();
        UUID authenticatedUserId = getAuthenticatedUserId(authentication);
        boolean isOwner = authenticatedUserId.equals(loan.getPerson().getId());

        if (!isOwner && !isAdmin(authentication)) {
            throw new SecurityException("You can only return your own loans");
        }

        loan.deleteThisLoan();
        loanRepository.save(loan);
        return true;
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"));
    }
}
```

- [ ] **Step 4: Rodar os testes (esperar passar)**

```bash
mvn test -Dtest=LoanServiceTest -q
```
Esperado: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/emakers/library_api/service/LoanService.java
git add src/test/java/com/emakers/library_api/service/LoanServiceTest.java
git commit -m "fix: bookLoan usa UUID, adiciona trava de duplicata, deleteLoan com @Transactional e ownership check"
```

---

## Task 8: Limpar controllers — Remover try-catch duplicado e imports mortos

**Problema:** Com o `GlobalExceptionHandler` (Task 2), os blocos try-catch em todos os controllers são dead code. `PersonController` também tem imports não utilizados da versão anterior.

**Files:**
- Modify: `src/main/java/com/emakers/library_api/controller/PersonController.java`
- Modify: `src/main/java/com/emakers/library_api/controller/LoanController.java`

- [ ] **Step 1: Reescrever PersonController limpo**

Substituir o conteúdo completo de `src/main/java/com/emakers/library_api/controller/PersonController.java`:

```java
package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.PersonPasswordChangeDto;
import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.request.PersonUpdateProfileDto;
import com.emakers.library_api.dto.response.PersonResponseDto;
import com.emakers.library_api.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints para gerenciamento de usuários (Pessoas)")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @Operation(summary = "Cadastra um novo Administrador", description = "Rota exclusiva para admins criarem outros usuários com privilégios de administrador.")
    @PostMapping("/admin")
    public ResponseEntity<PersonResponseDto> saveAdmin(@RequestBody @Valid PersonRecordDto personRecordDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.saveAdmin(personRecordDto));
    }

    @Operation(summary = "Cadastra um novo usuário", description = "Salva as informações de uma nova pessoa no banco de dados.")
    @PostMapping
    public ResponseEntity<PersonResponseDto> savePerson(@RequestBody @Valid PersonRecordDto personRecordDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.savePerson(personRecordDto));
    }

    @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista contendo todas as pessoas cadastradas.")
    @GetMapping
    public ResponseEntity<List<PersonResponseDto>> getAllPersons() {
        return ResponseEntity.ok(personService.getAllPersons());
    }

    @Operation(summary = "Busca um usuário pelo ID", description = "Retorna os detalhes de uma pessoa específica utilizando o seu UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<Object> getPersonById(@PathVariable UUID id) {
        Optional<PersonResponseDto> person = personService.getPersonById(id);
        return person.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found"));
    }

    @Operation(summary = "Atualiza o perfil de um usuário", description = "Atualiza nome, email e CEP. CPF e senha não são alterados aqui.")
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateProfile(@PathVariable UUID id,
                                                @RequestBody @Valid PersonUpdateProfileDto updateDto,
                                                Authentication authentication) {
        Optional<PersonResponseDto> updated = personService.updateProfile(id, updateDto, authentication);
        return updated.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found"));
    }

    @Operation(summary = "Altera a senha de um usuário", description = "Permite que um usuário autenticado altere sua própria senha.")
    @PutMapping("/{id}/change-password")
    public ResponseEntity<Object> changePassword(@PathVariable UUID id,
                                                 @RequestBody @Valid PersonPasswordChangeDto changeDto,
                                                 Authentication authentication) {
        Optional<PersonResponseDto> result = personService.changePassword(id, changeDto, authentication);
        return result.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found"));
    }

    @Operation(summary = "Deleta um usuário", description = "Remove permanentemente uma pessoa do banco de dados pelo seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletePerson(@PathVariable UUID id) {
        if (!personService.deletePerson(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person Not Found");
        }
        return ResponseEntity.ok("Person Deleted");
    }
}
```

- [ ] **Step 2: Reescrever LoanController limpo**

Substituir o conteúdo completo de `src/main/java/com/emakers/library_api/controller/LoanController.java`:

```java
package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.LoanRecordDto;
import com.emakers.library_api.dto.response.LoanResponseDto;
import com.emakers.library_api.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/loans")
@Tag(name = "Loans", description = "Endpoints para gerenciamento de empréstimos e devoluções")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(summary = "Realiza o empréstimo de um livro", description = "Cria um novo empréstimo vinculando uma pessoa e um livro pelos seus UUIDs.")
    @PostMapping
    public ResponseEntity<Object> bookLoan(@RequestBody @Valid LoanRecordDto loanRecordDto, Authentication authentication) {
        Optional<LoanResponseDto> result = loanService.bookLoan(loanRecordDto, authentication);
        return result.<ResponseEntity<Object>>map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book or Person not found"));
    }

    @Operation(summary = "Realiza a devolução de um livro", description = "Encerra um empréstimo ativo (soft delete) pelo ID do empréstimo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteLoan(@PathVariable UUID id, Authentication authentication) {
        if (!loanService.deleteLoan(id, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Loan Not Found");
        }
        return ResponseEntity.ok("Return successful");
    }
}
```

- [ ] **Step 3: Compilar e verificar**

```bash
mvn compile -q
```
Esperado: `BUILD SUCCESS` sem nenhum erro de compilação.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/emakers/library_api/controller/PersonController.java
git add src/main/java/com/emakers/library_api/controller/LoanController.java
git commit -m "refactor: controllers delegam tratamento de excecoes ao GlobalExceptionHandler, remove imports mortos"
```

---

## Task 9: Verificação Final — Build, testes e smoke test

- [ ] **Step 1: Rodar todos os testes**

```bash
mvn test -q
```
Esperado: `BUILD SUCCESS`. Verificar que `GlobalExceptionHandlerTest`, `JwtServiceTest`, `PersonServiceTest` e `LoanServiceTest` passam.

- [ ] **Step 2: Compilar o projeto completo**

```bash
mvn clean compile -q
```
Esperado: zero erros de compilação.

- [ ] **Step 3: Verificar que o contexto Spring sobe (requer DB)**

Se o banco estiver disponível (via docker-compose):
```bash
docker-compose up -d
mvn spring-boot:run &
sleep 15
curl -s http://localhost:8080/v3/api-docs | grep "openapi"
```
Esperado: JSON com `"openapi": "3.x.x"`.

- [ ] **Step 4: Smoke test — login + empréstimo**

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"marco@ufla.br","password":"senha123"}' | jq -r '.token')

# Listar livros (público)
BOOK_ID=$(curl -s http://localhost:8080/books | jq -r '.[0].id')

# Pegar UUID do usuário
PERSON_ID=$(curl -s http://localhost:8080/users \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Criar empréstimo (agora usa UUID)
curl -s -X POST http://localhost:8080/loans \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"bookId\":\"$BOOK_ID\",\"personId\":\"$PERSON_ID\"}"
```
Esperado: `201 Created` com o LoanResponseDto.

- [ ] **Step 5: Commit final**

```bash
git add -A
git commit -m "test: adiciona testes unitarios para PersonService, LoanService, JwtService e GlobalExceptionHandler"
```

---

## Resumo das Correções

| # | Bug | Severidade | Task |
|---|---|---|---|
| 1 | SecurityConfig regras invertidas (loans bloqueado, users/{id} morto) | CRÍTICO | Task 4 |
| 2 | saveAdmin sem validação de email/CPF/CEP | ALTO | Task 6 |
| 3 | Sem trava de empréstimo duplicado | ALTO | Task 7 |
| 4 | updateProfile sem validação de CEP | MÉDIO | Task 6 |
| 5 | Stale JWT após troca de email (ownership por email) | MÉDIO | Task 3 + 6 + 7 |
| 6 | SecurityException para senha errada → HTTP 403 (deveria ser 400) | MÉDIO | Task 6 |
| 7 | deleteLoan sem @Transactional e sem ownership | MÉDIO | Task 7 |
| 8 | ddl-auto=update + Flyway conflitam | MÉDIO | Task 1 |
| 9 | LoanRecordDto usa título/CPF (ambíguo + PII) | MÉDIO | Task 5 |
| 10 | Sem @ControllerAdvice (try-catch duplicado nos controllers) | REFACTOR | Task 2 + 8 |
