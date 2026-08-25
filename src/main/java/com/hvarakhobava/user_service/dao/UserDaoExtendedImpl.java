package com.hvarakhobava.user_service.dao;

import com.hvarakhobava.user_service.model.User;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Hanna Varakhobava
 */

public class UserDaoExtendedImpl implements UserDaoExtended{

    private final JdbcAggregateTemplate aggregateTemplate;

    public UserDaoExtendedImpl(JdbcAggregateTemplate aggregateTemplate) {
        this.aggregateTemplate = aggregateTemplate;
    }

    public User insert(User user) {
        return aggregateTemplate.insert(user);
    }
}
