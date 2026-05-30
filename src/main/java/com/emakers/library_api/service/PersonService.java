package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.PersonRecordDto;
import com.emakers.library_api.dto.response.PersonResponseDto;
import com.emakers.library_api.dto.response.ViaCepResponseDto;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import com.emakers.library_api.repositores.PersonRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final ViaCepService viaCepService;

    public PersonService(PersonRepository personRepository, ViaCepService viaCepService) {
        this.personRepository = personRepository;
        this.viaCepService = viaCepService;
    }

    public PersonResponseDto saveAdmin(PersonRecordDto personRecordDto) {
        var personModel = new PersonModel(personRecordDto);
        personModel.setRole(UserRole.ADMIN);
        return new PersonResponseDto(personRepository.save(personModel));
    }

    public PersonResponseDto savePerson(PersonRecordDto personRecordDto) {
        if (personRepository.existsByEmail(personRecordDto.email())) {
            throw new IllegalArgumentException("This E-mail is already registered.");
        }
        if (personRepository.existsByCpf(personRecordDto.cpf())) {
            throw new IllegalArgumentException("This CPF is already registered.");
        }
        ViaCepResponseDto zipCode = viaCepService.checkCep(personRecordDto.zipCode());
        if (zipCode == null) {
            throw new IllegalArgumentException("The provided zip code does not exist.");
        }
        var personModel = new PersonModel(personRecordDto);
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

    public Optional<PersonResponseDto> updatePerson(UUID id, PersonRecordDto personRecordDto, Authentication authentication) {
        Optional<PersonModel> personModelOptional = personRepository.findById(id);
        if (personModelOptional.isEmpty()) {
            return Optional.empty();
        }
        PersonModel personModel = personModelOptional.get();
        String authenticationEmail = authentication.getName();
        boolean isNotOwner = !authenticationEmail.equals(personModel.getEmail());
        boolean isNotAdmin = authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"));

        if (isNotOwner && isNotAdmin) {
            throw new SecurityException("You can only change your own data!");
        }

        personModel.updatePerson(personRecordDto);
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
}
