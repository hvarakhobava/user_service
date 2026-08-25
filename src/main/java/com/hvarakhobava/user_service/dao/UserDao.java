package com.hvarakhobava.user_service.dao;

import com.hvarakhobava.user_service.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDao extends UserDaoExtended, CrudRepository<User, UUID>, PagingAndSortingRepository<User, UUID>  {
    Optional<User> findByEmail(String email);
}