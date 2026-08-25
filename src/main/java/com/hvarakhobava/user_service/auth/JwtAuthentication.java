package com.hvarakhobava.user_service.auth;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * @author Hanna Varakhobava
 */
@AllArgsConstructor
@Builder
@ToString
public class JwtAuthentication implements Authentication {
    private final Object jti;
    private final Object userId;
    private final Object role;
    @lombok.Builder.Default
    private boolean authenticated = true;

    public JwtAuthentication(Object jti, Object userId, Object role) {
        this.jti = jti;
        this.userId = userId;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toString()));
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }

    @Override
    public @Nullable Object getDetails() {
        return null;
    }

    @Override
    public @Nullable Object getPrincipal() {
        return userId;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
