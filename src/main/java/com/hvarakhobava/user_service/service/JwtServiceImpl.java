package com.hvarakhobava.user_service.service;

import com.hvarakhobava.user_service.dto.JwtTokenConfig;
import com.hvarakhobava.user_service.model.User;
import com.hvarakhobava.user_service.util.RSAUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author Hanna Varakhobava
 */
@Slf4j
@Service
public class JwtServiceImpl implements JwtService<Claims> {
    private final Environment env;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtServiceImpl(Environment env) throws InvalidKeySpecException {
        this.env = env;

        String pk = env.getProperty("PRIVATE_KEY");
        String pubKey = env.getProperty("PUBLIC_KEY");

        this.privateKey = RSAUtils.loadPrivateKey(pk);
        this.publicKey = RSAUtils.loadPublicKey(pubKey);// PemContent.of(env.getProperty("PUBLIC_KEY")).getCertificates().getFirst().getPublicKey();
    }

    @Override
    public String issueToken(JwtTokenConfig tokenConfig) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(tokenConfig.sub())
                .claims(tokenConfig.claims())
                .issuedAt(Date.from(tokenConfig.iat()))
                .expiration(Date.from(tokenConfig.exp()))
                .signWith(privateKey)
                .compact();
    }

    @Override
    public Claims verifyAndParse(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
