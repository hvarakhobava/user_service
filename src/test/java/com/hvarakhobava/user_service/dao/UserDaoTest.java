package com.hvarakhobava.user_service.dao;

import com.hvarakhobava.user_service.model.User;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.AutoConfigureDataJdbc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    void findByEmail() {
        User user = User.builder()
                .email(RandomString.make(4))
                .passwordHash("abc")
                .role(User.Role.BUYER).build();

        User saved = userDao.save(user);
        Optional<User> found = userDao.findById(saved.getId());

        assertTrue(found.isPresent());
        User foundUser = found.get();

        assertEquals(foundUser.getEmail(), user.getEmail());
        assertNotNull(foundUser.getId());
        assertNotNull(foundUser.getCreatedAt());
        assertEquals(foundUser.getCreatedAt(), foundUser.getModifiedAt());
    }
}