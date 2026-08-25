package com.hvarakhobava.user_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * @author Hanna Varakhobava
 */
@Table("buyer_profiles")
public class BuyerProfile {
    @Id
    private Long id;
    private String userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String billingAddress;
    private String shippingAddress;
    private String phone;
    private Instant createdAt;
    private Instant modifiedAt;
}
