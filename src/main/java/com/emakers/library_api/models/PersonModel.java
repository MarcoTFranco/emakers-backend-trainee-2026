package com.emakers.library_api.models;

import com.emakers.library_api.dto.request.PersonRecordDto;
import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Column(unique = true, nullable = false, length = 14)
    private String cpf;

    private String zipCode;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    public PersonModel() {
    }

    public PersonModel(PersonRecordDto personRecordDto, PasswordEncoder passwordEncoder) {
        this.name = personRecordDto.name();
        this.cpf = personRecordDto.cpf();
        this.zipCode = personRecordDto.zipCode();
        this.email = personRecordDto.email().toLowerCase().trim();
        this.password = passwordEncoder.encode(personRecordDto.password());
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

    public void setName(String name) {
        this.name = name;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void updateProfile(String name, String zipCode, String email) {
        this.name = name;
        this.zipCode = zipCode;
        this.email = email.toLowerCase().trim();
    }
}
