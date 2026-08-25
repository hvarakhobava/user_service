package com.hvarakhobava.user_service.service;

import com.hvarakhobava.user_service.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    User createUser(User user);
    void updateUser(User user);
    Optional<User> findUserById(UUID id);
    Optional<User> findActiveUserById(UUID id);
    Optional<User> findUserByEmail(String email);
    void deleteUserProfile(UUID id, User.Role profileRole);
    void deleteUser(UUID id);
    Collection<User> list(Pageable pageable);
}
