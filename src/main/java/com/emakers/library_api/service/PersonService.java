package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.PersonPasswordChangeDto;
import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.request.PersonUpdateProfileDto;
import com.emakers.library_api.dto.response.PersonResponseDto;
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
