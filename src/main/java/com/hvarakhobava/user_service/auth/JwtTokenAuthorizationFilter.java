package com.hvarakhobava.user_service.auth;

import com.hvarakhobava.user_service.dto.UserDTO;
import com.hvarakhobava.user_service.model.User;
import com.hvarakhobava.user_service.service.AuthService;
import com.hvarakhobava.user_service.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Hanna Varakhobava
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenAuthorizationFilter extends OncePerRequestFilter {
    private final JwtService<Claims> jwtService;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Filtering request by access token authorization, {}", request.getRequestId());
        Optional<String> tokenOptional = extractToken(request);

        tokenOptional
                .map(jwtService::verifyAndParse)
                .filter(claims -> {
                    log.info("{}", claims);
                    return true;
                })
                .filter(this::filterClaims)
                .map(this::toJwtAuthentication)
                .ifPresent(auth -> {
                    log.info("Authenticated, {}", auth);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream().flatMap(Arrays::stream)
                .filter(cookie -> cookie.getName().equals("access_token"))
                .findFirst()
                .map(Cookie::getValue)
                .or(() -> Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                        .map(s -> StringUtils.substringAfter(s, "Bearer ")));
    }

    private JwtAuthentication toJwtAuthentication(Claims claims) {
        UUID userId = Optional.ofNullable(claims.getSubject())
                .map(UUID::fromString).orElse(null);
        User.Role role = Optional.ofNullable(claims.get("role"))
                .map(o -> User.Role.valueOf(o.toString()))
                .orElse(null);
        String jti = claims.getId();

        return jti != null && userId != null && role != null ?
                JwtAuthentication.builder()
                .jti(jti)
                .userId(userId)
                .role(role)
                .build() :
                null;
    }

    private boolean filterClaims(Claims claims) {
        return !blacklisted(claims.getId());
    }

    private boolean blacklisted(Object jti) {
        return authService.isBlacklisted(jti);
    }
}
