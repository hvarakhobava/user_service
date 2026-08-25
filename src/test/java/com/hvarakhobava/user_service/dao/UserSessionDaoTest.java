package com.hvarakhobava.user_service.dao;

import com.hvarakhobava.user_service.model.UserSession;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserSessionDaoTest {

    @Autowired
    private UserSessionDao userSessionDao;
    @Autowired
    private UserDao userDao;

    @Test
    public void testSave() {
        userDao.findByEmail("a@b.com").ifPresent(user -> {
            UserSession session = UserSession.builder()
                    .userId(user.getId())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .refreshTokenHash(RandomString.make(128))
                    .deviceId("asdf")
                    .ip("192.168.12.13")
                    .build();
            Long id = userSessionDao.save(session).getId();
            Optional<UserSession> found = userSessionDao.findById(id);
            assertTrue(found.isPresent());
            UserSession foundSession = found.get();
            assertEquals(session.getDeviceId(), foundSession.getDeviceId());
            assertEquals(session.getIp(), foundSession.getIp());
            assertNotNull(foundSession.getCreatedAt());
            assertEquals(foundSession.getCreatedAt(), foundSession.getModifiedAt());
        });
    }

}