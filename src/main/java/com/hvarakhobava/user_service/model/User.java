package com.hvarakhobava.user_service.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Hanna Varakhobava
 */
@Data
@Builder
@Table("users")
public class User {
    @Id
    private UUID id;
    private String email;
    private String passwordHash;
    private User.Role role;
    @Builder.Default
    private User.Status status = Status.NEW;
    @ReadOnlyProperty
    private LocalDateTime createdAt;
    @ReadOnlyProperty
    private LocalDateTime modifiedAt;
//    private MerchantProfile merchantProfile;
//    private BuyerProfile buyerProfile;
//    private ThirdPartyId thirdPartyId;

    public enum Role {
        BUYER,
        MERCHANT,
        ADMIN
    }

    public enum Status {
        NEW,
        VERIFICATION_REQUIRED,
        VERIFIED,
        BLOCKED,
        DELETED
    }
}
