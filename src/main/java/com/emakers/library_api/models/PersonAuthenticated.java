package com.emakers.library_api.models;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class PersonAuthenticated implements UserDetails {

    private final PersonModel personModel;

    public PersonAuthenticated(PersonModel personModel) {
        this.personModel = personModel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(personModel.getRole() == UserRole.ADMIN) {
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
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
