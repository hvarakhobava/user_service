package com.hvarakhobava.user_service.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserSessionDTO(Long id, UUID userId, String refreshToken, boolean revoked) {
}
