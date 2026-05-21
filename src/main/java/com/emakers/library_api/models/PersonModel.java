package com.emakers.library_api.models;

import com.emakers.library_api.dto.PersonRecordDto;
import jakarta.persistence.*;
import jakarta.validation.Valid;

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

    public PersonModel() {
    }

    public PersonModel(PersonRecordDto personRecordDto) {
        this.name = personRecordDto.name();
        this.cpf = personRecordDto.cpf();
        this.zipCode = personRecordDto.zipCode();
        this.email = personRecordDto.email();
        this.password = personRecordDto.password();
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

    private void setName(String name) {
        this.name = name;
    }
    private void setCpf(String cpf) {
        this.cpf = cpf;
    }
    private void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    private void setEmail(String email) {
        this.email = email;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    public void updatePerson(@Valid PersonRecordDto personRecordDto) {
        this.name = personRecordDto.name();
        this.cpf = personRecordDto.cpf();
        this.zipCode = personRecordDto.zipCode();
        this.email = personRecordDto.email();
        this.password = personRecordDto.password();
    }
}
