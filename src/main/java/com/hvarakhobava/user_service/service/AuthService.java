package com.hvarakhobava.user_service.service;

import com.hvarakhobava.user_service.dto.TokenPair;
import com.hvarakhobava.user_service.dto.UserDTO;
import com.hvarakhobava.user_service.model.AuthProvider;

import java.util.Map;

public interface AuthService {
    UserDTO registerUser(UserDTO userDTO);
    TokenPair authenticateUser(String email, String passwordHash);
    TokenPair authenticateUser(AuthProvider provider, Map<String, String> params);
    TokenPair refreshAuthToken(String refreshTokenHash);
    void invalidateSession(TokenPair tokenPair);

    boolean isBlacklisted(Object jti);
}
