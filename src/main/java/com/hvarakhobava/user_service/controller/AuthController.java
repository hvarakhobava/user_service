package com.hvarakhobava.user_service.controller;

import com.hvarakhobava.user_service.controller.dto.LoginRequest;
import com.hvarakhobava.user_service.controller.dto.RegisterUserRequest;
import com.hvarakhobava.user_service.dto.TokenPair;
import com.hvarakhobava.user_service.dto.UserDTO;
import com.hvarakhobava.user_service.model.AuthProvider;
import com.hvarakhobava.user_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ua_parser.Client;
import ua_parser.Parser;
import ua_parser.UserAgent;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * @author Hanna Varakhobava
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Object registerUser(@Valid @RequestBody RegisterUserRequest request) {
        log.info("Register: {}", request.email());
        return authService.registerUser(UserDTO.builder()
                .email(request.email())
                .password(request.password())
                .build());
    }

    @PostMapping("/login")
    public TokenPair login(@RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        log.info("Login request: {}, user agent: {}", loginRequest.email(), userAgent);
        Client parsed = new Parser().parse(userAgent);

        TokenPair tokens = authService.authenticateUser(loginRequest.email(), loginRequest.password());
        // set cookies
        setAuthorizationCookies(httpResponse, tokens);
        return tokens;
    }

    @GetMapping("/callback/google")
    public void idProviderCallback(@RequestParam Map<String, String> params) {
        log.info("Callback[Google]: {}", params);
        authService.authenticateUser(AuthProvider.GOOGLE, params);
    }

    @PostMapping("/invalidate/refreshToken")
    public TokenPair refreshToken(@CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
                               @RequestHeader(name = "X-Refresh-Token", required = false) String headerRefreshToken,
                               HttpServletResponse response) {
        log.info("Refresh tokenPair request");
        Optional<String> tokenOptional = Optional.ofNullable(cookieRefreshToken)
                .or(() -> Optional.ofNullable(headerRefreshToken));

        if (tokenOptional.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
        TokenPair tokenPair = authService.refreshAuthToken(tokenOptional.get());
        setAuthorizationCookies(response, tokenPair);

        return tokenPair;
    }

    @PutMapping("/invalidate")
    public void invalidateSession(@CookieValue(name = "access_token", required = false) String accessToken,
                                  @CookieValue(name = "refresh_token", required = false) String refreshToken,
                                  @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                  @RequestHeader(name = "X-Refresh-Token", required = false) String refreshHeader,
                                  HttpServletResponse response) throws IOException {
        log.info("Invalidate request: \n{}", String.join("\n",
                accessToken, refreshToken, authorization, refreshHeader));
        TokenPair tokenPair;
        if (accessToken != null && refreshToken != null) {
            tokenPair = new TokenPair(accessToken, refreshToken);
        } else if (authorization != null && refreshHeader != null) {
            tokenPair = new TokenPair(StringUtils.substringAfter(authorization, "Bearer "), refreshHeader);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid authentication");
            return;
        }

        authService.invalidateSession(tokenPair);
        setAuthorizationCookies(response, null);
    }


    private static void setAuthorizationCookies(HttpServletResponse httpResponse, TokenPair tokens) {
        Optional<String> accessOpt = Optional.ofNullable(tokens).map(TokenPair::accessToken);
        Optional<String> refreshOpt = Optional.ofNullable(tokens).map(TokenPair::refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessOpt.orElse(null))
                .httpOnly(true)
//                .secure(true)
                .sameSite("Strict")
                .maxAge(Duration.ofMinutes(accessOpt.isEmpty() ? 0 : 15))
                .path("/")
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshOpt.orElse(null))
                .httpOnly(true)
//                .secure(true)
                .sameSite("Strict")
                .maxAge(Duration.ofDays(refreshOpt.isEmpty() ? 0 : 7))
                .path("/auth/invalidate")
                .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
