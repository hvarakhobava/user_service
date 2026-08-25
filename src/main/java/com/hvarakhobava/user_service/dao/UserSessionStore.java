package com.hvarakhobava.user_service.dao;

import com.hvarakhobava.user_service.model.UserSession;

import java.time.Duration;
import java.util.Optional;

public interface UserSessionStore {
    void blacklist(String sessionId, Duration ttl);
    boolean isBlacklisted(String sessionId);

    Optional<UserSession> get(Long sessionId);
    void put(UserSession userSession, Duration ttl);

    void remove(UserSession s);
}
