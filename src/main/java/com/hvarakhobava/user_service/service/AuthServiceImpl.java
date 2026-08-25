package com.hvarakhobava.user_service.service;

import com.hvarakhobava.user_service.dao.UserSessionDao;
import com.hvarakhobava.user_service.dao.UserSessionStore;
import com.hvarakhobava.user_service.dto.JwtTokenConfig;
import com.hvarakhobava.user_service.dto.TokenPair;
import com.hvarakhobava.user_service.dto.UserDTO;
import com.hvarakhobava.user_service.dto.UserSessionDTO;
import com.hvarakhobava.user_service.model.AuthProvider;
import com.hvarakhobava.user_service.model.User;
import com.hvarakhobava.user_service.model.UserSession;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author Hanna Varakhobava
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private static final Duration REFRESH_TOKEN_EXPIRATION_PERIOD = Duration.of(7, ChronoUnit.DAYS);
    private static final Duration ACCESS_TOKEN_EXPIRATION_PERIOD = Duration.of(15, ChronoUnit.MINUTES);

    private final UserService userService;
    private final JwtService<Claims> jwtService;
    private final UserSessionDao userSessionDao;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final UserSessionStore sessionStore;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        User user = userService.createUser(User.builder()
                .email(userDTO.email())
                .role(Optional.ofNullable(userDTO.role()).orElse(User.Role.BUYER))
                .passwordHash(encoder.encode(userDTO.password()))
                .build());
        return UserDTO.builder()
                .id(user.getId())
                .role(user.getRole())
                .status(user.getStatus())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public TokenPair authenticateUser(String email, String password) {
        User found = userService.findUserByEmail(email)
                .filter(user -> passwordMatches(password, user.getPasswordHash()))
                .orElseThrow(() -> new RuntimeException("User with provided email and password does not exists"));

        return createUserSession(found);
    }

    @Override
    public TokenPair authenticateUser(AuthProvider provider, Map<String, String> params) {
        return null;
    }

    private @NonNull TokenPair createUserSession(User user) {
        Instant issueDate = Instant.now();
        String accessToken = issueAccessToken(user, issueDate);
        String refreshToken = issueRefreshToken(user, issueDate);

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .refreshTokenHash(encoder.encode(refreshToken))
                .expiresAt(LocalDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(REFRESH_TOKEN_EXPIRATION_PERIOD))
//                .ip()
//                .deviceId()
//                .userAgent()
                .build();
        userSessionDao.save(session);
        sessionStore.put(session, ACCESS_TOKEN_EXPIRATION_PERIOD);

        return new TokenPair(accessToken, refreshToken + "|" + session.getId());
    }

    private String issueRefreshToken(User found, Instant issueDate) {
        return UUID.randomUUID().toString();
    }

    private String issueAccessToken(User found, Instant issueDate) {
        return jwtService.issueToken(new JwtTokenConfig(found.getId().toString(),
                Map.of("role", found.getRole()),
                issueDate,
                issueDate.plus(ACCESS_TOKEN_EXPIRATION_PERIOD)));
    }

    @Override
    @Transactional
    public void invalidateSession(TokenPair tokenPair) {
        UserSessionDTO result = parseSession(tokenPair.refreshToken());
        revokeSession(result.id(), result.refreshToken());

        Claims claims = jwtService.verifyAndParse(tokenPair.accessToken());
        String jti = claims.get("jti").toString();
        Date expirationDate = claims.getExpiration();

        blacklistJti(jti, Duration.between(Instant.now(), expirationDate.toInstant()));
    }

    private static @NonNull UserSessionDTO parseSession(String refreshToken) {
        String[] tokenParts = refreshToken.split("\\|");
        if (tokenParts.length != 2 || !StringUtils.isNumeric(tokenParts[1])) {
            throw new RuntimeException("Invalid token format!");
        }

        String refresh = tokenParts[0];
        Long sessionId = Long.valueOf(tokenParts[1]);
        return UserSessionDTO.builder().id(sessionId).refreshToken(refresh).build();
    }

    private UserSession revokeSession(Long sessionId, String refreshToken) {
        UserSession userSession = sessionStore.get(sessionId)
                .or(() -> userSessionDao.findById(sessionId))
                .filter(session -> passwordMatches(refreshToken, session.getRefreshTokenHash()))
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        sessionStore.remove(userSession);
        userSessionDao.revoke(userSession.getId());

        return userSession;
    }

    private void blacklistJti(String jti, Duration ttl) {
        log.info("Blacklisting jti {}", jti);
        sessionStore.blacklist(jti, ttl);
    }

    @Override
    @Transactional
    public TokenPair refreshAuthToken(String refreshToken) {
        UserSessionDTO sessionDTO = parseSession(refreshToken);
        UserSession oldSession = revokeSession(sessionDTO.id(), sessionDTO.refreshToken());
        Optional<User> user = userService.findActiveUserById(oldSession.getUserId());

        if (!oldSession.isActive() || user.isEmpty()) {
            log.warn("An attempt to refresh token on inactive session or user detected");
            return null;
        }
        return createUserSession(user.get());
    }

    private boolean passwordMatches(String password, String passwordHash) {
        return encoder.matches(password, passwordHash);
    }

    @Override
    public boolean isBlacklisted(Object jti) {
        return sessionStore.isBlacklisted(String.valueOf(jti));
    }
}
