package com.hvarakhobava.user_service.model;

import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Hanna Varakhobava
 */
@Table("third_party_ids")
public class ThirdPartyId {
    private Long id;
    private String userId;
    private String provider;
    private String externalId;
    private ThirdPartyId.Status status;

    public enum Status {
        ACTIVE,
        SUSPENDED
    }
}
