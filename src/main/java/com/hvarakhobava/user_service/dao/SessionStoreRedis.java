package com.hvarakhobava.user_service.dao;

import com.hvarakhobava.user_service.model.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * @author Hanna Varakhobava
 */
@Repository
@RequiredArgsConstructor
public class SessionStoreRedis implements UserSessionStore{
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String SESSION_PREFIX = "session:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void blacklist(String sessionId, Duration ttl) {
        redisTemplate.opsForValue()
                .set("blacklist:" + sessionId, sessionId, ttl);
    }

    public boolean isBlacklisted(String sessionId) {
        return redisTemplate.opsForValue()
                .getOperations().hasKey(BLACKLIST_PREFIX + sessionId);
    }

    @Override
    public Optional<UserSession> get(Long sessionId) {
        return Optional.ofNullable((UserSession) redisTemplate.opsForValue()
                .get(SESSION_PREFIX + sessionId));
    }

    @Override
    public void put(UserSession userSession, Duration ttl) {
        redisTemplate.opsForValue().set(SESSION_PREFIX + userSession.getId(), userSession, ttl);
    }

    @Override
    public void remove(UserSession s) {
        redisTemplate.opsForValue().getAndDelete(SESSION_PREFIX + s.getId());
    }
}
