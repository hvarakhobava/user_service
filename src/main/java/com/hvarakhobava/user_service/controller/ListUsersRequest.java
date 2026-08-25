package com.hvarakhobava.user_service.controller;

import org.springframework.data.domain.Sort;

public record ListUsersRequest(int size, int pageNumber, String sortBy, Sort.Direction direction) {
}
