package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.PersonPasswordChangeDto;
import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.request.PersonUpdateProfileDto;
import com.emakers.library_api.models.PersonModel;
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

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock PersonRepository personRepository;
    @Mock ViaCepService viaCepService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks PersonService personService;

    private UUID personId;

    @BeforeEach
    void setUp() {
        personId = UUID.randomUUID();
    }

    private PersonModel mockPerson(UUID id, String email, String hashedPassword) {
        PersonModel person = mock(PersonModel.class);
        lenient().when(person.getId()).thenReturn(id);
        lenient().when(person.getEmail()).thenReturn(email);
        lenient().when(person.getPassword()).thenReturn(hashedPassword);
        return person;
    }

    private Authentication mockAuthWithUserId(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("userId")).thenReturn(userId.toString());
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(jwt);
        lenient().when(auth.getAuthorities()).thenReturn(Collections.emptyList());
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
        when(personRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(personRepository.existsByCpf("111.444.777-35")).thenReturn(true);
        PersonRecordDto dto = new PersonRecordDto("Admin", "111.444.777-35", "35500-000", "admin@test.com", "admin123");

        assertThatThrownBy(() -> personService.saveAdmin(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void saveAdmin_throwsWhenInvalidCep() {
        when(personRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
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
        PersonModel person = mockPerson(personId, "user@test.com", "$2a$hash");
        Authentication auth = mockAuthWithUserId(personId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(viaCepService.checkCep("00000-000")).thenReturn(null);
        PersonUpdateProfileDto dto = new PersonUpdateProfileDto("Name", "00000-000", "user@test.com");

        assertThatThrownBy(() -> personService.updateProfile(personId, dto, auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zip code");
    }

    @Test
    void updateProfile_throwsWhenEmailTaken() {
        PersonModel person = mockPerson(personId, "user@test.com", "$2a$hash");
        Authentication auth = mockAuthWithUserId(personId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(personRepository.existsByEmailIgnoreCase("other@test.com")).thenReturn(true);
        PersonUpdateProfileDto dto = new PersonUpdateProfileDto("Name", "35500-000", "other@test.com");

        assertThatThrownBy(() -> personService.updateProfile(personId, dto, auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void updateProfile_throwsSecurityWhenNotOwnerNotAdmin() {
        PersonModel person = mockPerson(personId, "user@test.com", "$2a$hash");
        Authentication authAsOther = mockAuthWithUserId(UUID.randomUUID());
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        PersonUpdateProfileDto dto = new PersonUpdateProfileDto("Name", "35500-000", "user@test.com");

        assertThatThrownBy(() -> personService.updateProfile(personId, dto, authAsOther))
                .isInstanceOf(SecurityException.class);
    }

    // --- changePassword ---

    @Test
    void changePassword_throwsIllegalArgWhenPasswordWrong() {
        PersonModel person = mockPerson(personId, "user@test.com", "$2a$hash");
        Authentication auth = mockAuthWithUserId(personId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("wrongPass", "$2a$hash")).thenReturn(false);
        PersonPasswordChangeDto dto = new PersonPasswordChangeDto("wrongPass", "newPassword123");

        assertThatThrownBy(() -> personService.changePassword(personId, dto, auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void changePassword_throwsSecurityWhenNotOwner() {
        PersonModel person = mockPerson(personId, "user@test.com", "$2a$hash");
        Authentication authAsOther = mockAuthWithUserId(UUID.randomUUID());
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> personService.changePassword(personId, new PersonPasswordChangeDto("x", "newpassword1"), authAsOther))
                .isInstanceOf(SecurityException.class);
    }
}
