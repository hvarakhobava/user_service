package com.hvarakhobava.user_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * @author Hanna Varakhobava
 */
@Table("merchant_profiles")
public class MerchantProfile {
    @Id
    private Long id;
    private String userId;
    private String companyName;
    private String companyAddress;
    private String companyRegistrationNumber;
    private String companyTaxId;
    private String companyEmail;
    private String companyPhone;
    private Instant createdAt;
    private Instant modifiedAt;
}
