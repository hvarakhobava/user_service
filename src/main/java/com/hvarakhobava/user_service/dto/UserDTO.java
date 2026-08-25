package com.hvarakhobava.user_service.dto;

import com.hvarakhobava.user_service.model.User;
import lombok.Builder;

import java.util.UUID;

/**
 * @author Hanna Varakhobava
 */
@Builder
public record UserDTO (
        UUID id,
        String email,
        String password,
        User.Status status,
        User.Role role
){
}
