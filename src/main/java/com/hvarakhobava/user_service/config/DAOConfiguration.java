package com.hvarakhobava.user_service.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.sql.DataSource;
import java.util.Optional;

import static com.hvarakhobava.user_service.util.JsonUtils.OBJECT_MAPPER;

/**
 * @author Hanna Varakhobava
 */
@Configuration
//@EnableJdbcAuditing
@EnableJdbcRepositories("com.hvarakhobava.user_service.dao")
public class DAOConfiguration {

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:liquibase/changelog-master.xml");
        liquibase.setDataSource(dataSource);
        return liquibase;
    }

    @ConfigurationProperties(prefix = "datasource")
    public record DatasourceProperties(
            String username,
            String password,
            String url,
            String schema
    ){}

    @Bean
    public DataSource dataSource(DatasourceProperties properties) {
        return DataSourceBuilder.create()
                .url(properties.url())
                .username(properties.username)
                .password(properties.password)
                .build();
    }

    @Bean
    RedisConnectionFactory connectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setStringSerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        return template;
    }
}
