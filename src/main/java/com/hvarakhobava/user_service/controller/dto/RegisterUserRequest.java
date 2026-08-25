package com.hvarakhobava.user_service.controller.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @author Hanna Varakhobava
 */
public record RegisterUserRequest(@NotNull String email, @NotNull String password) {
}
