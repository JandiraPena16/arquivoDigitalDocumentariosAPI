package com.arquivodigital.security;

import com.arquivodigital.entity.Utilizador;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final Utilizador utilizador;

    public UserDetailsImpl(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + utilizador.getRole().name()));
    }

    @Override public String getPassword() { return utilizador.getPassword(); }
    @Override public String getUsername() { return utilizador.getEmail(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return utilizador.isAtivo(); }

    public Long getId() { return utilizador.getId(); }
}
