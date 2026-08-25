package com.hvarakhobava.user_service.service;

import com.hvarakhobava.user_service.dao.UserDao;
import com.hvarakhobava.user_service.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Hanna Varakhobava
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Set<User.Status> INACTIVE_STATUSES =
            Set.of(User.Status.DELETED, User.Status.BLOCKED);
    private final UserDao userDao;

    @Override
    public User createUser(User user) {
        try {
            return userDao.save(user);
        } catch (DuplicateKeyException e) {
            log.info("Sql exception, reason: {}",
                    e.getMessage());
            throw new RuntimeException("User with email %s already exists".formatted(user.getEmail()));
        }
    }

    @Override
    public void updateUser(User user) {
        User found = userDao.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        BeanUtils.copyProperties(user, found, "email", "passwordHash");
        userDao.save(user);
    }

    @Override
    public Optional<User> findUserById(UUID id) {
        return userDao.findById(id);
    }

    @Override
    public Optional<User> findActiveUserById(UUID id) {
        return userDao.findById(id)
                .filter(user -> !INACTIVE_STATUSES.contains(user.getStatus()));
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userDao.findByEmail(email);
    }

    @Override
    public Collection<User> list(Pageable pageable) {
        return userDao.findAll(pageable).getContent();
    }

    @Override
    public void deleteUserProfile(UUID id, User.Role profileRole) {
        //todo: implement merchant and buyer dao
    }

    @Override
    public void deleteUser(UUID id) {
        //todo: implement patch method
    }
}
