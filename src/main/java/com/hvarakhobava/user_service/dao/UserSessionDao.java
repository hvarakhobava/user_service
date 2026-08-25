package com.hvarakhobava.user_service.dao;

import com.hvarakhobava.user_service.model.UserSession;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionDao extends CrudRepository<UserSession, Long>, PagingAndSortingRepository<UserSession, Long> {
    Optional<List<UserSession>> findByUserId(String userId);
    Optional<UserSession> findByIdAndRefreshTokenHash(Long id, String tokenHash);

    @Modifying
    @Query("UPDATE user_sessions SET revoked = TRUE WHERE id = :id")
    void revoke(@Param("id") Long id);
}
