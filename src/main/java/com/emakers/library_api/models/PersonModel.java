package com.emakers.library_api.models;

import com.emakers.library_api.dto.request.PersonRecordDto;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "TB_PERSON")
public class PersonModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    private String cpf;

    private String zipCode;

    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    public PersonModel() {
    }

    public PersonModel(PersonRecordDto personRecordDto) {
        this.name = personRecordDto.name();
        this.cpf = personRecordDto.cpf();
        this.zipCode = personRecordDto.zipCode();
        this.email = personRecordDto.email();
        this.password = new BCryptPasswordEncoder().encode(personRecordDto.password());
        this.role = UserRole.USER;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void updatePerson(@Valid PersonRecordDto personRecordDto) {
        this.name = personRecordDto.name();
        this.cpf = personRecordDto.cpf();
        this.zipCode = personRecordDto.zipCode();
        this.email = personRecordDto.email();
        this.password = new BCryptPasswordEncoder().encode(personRecordDto.password());
    }
}
