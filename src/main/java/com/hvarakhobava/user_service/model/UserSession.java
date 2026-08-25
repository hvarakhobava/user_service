package com.hvarakhobava.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Hanna Varakhobava
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("user_sessions")
public class UserSession {
    @Id
    private Long id;
    private UUID userId;
    private String refreshTokenHash;
    private LocalDateTime expiresAt;
    private String ip;
    private String deviceId;
    private String userAgent;
    @Builder.Default
    private Boolean revoked = false;
    @ReadOnlyProperty
    private LocalDateTime createdAt;
    @ReadOnlyProperty
    private LocalDateTime modifiedAt;

    public boolean isActive() {
        return this.getExpiresAt().isAfter(LocalDateTime.now()) &&
                !this.getRevoked();
    }
}
