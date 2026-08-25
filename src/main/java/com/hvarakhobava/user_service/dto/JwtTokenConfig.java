package com.hvarakhobava.user_service.dto;

import java.time.Instant;
import java.util.Map;

public record JwtTokenConfig(String sub, Map<String, ?> claims, Instant iat, Instant exp) {
}
