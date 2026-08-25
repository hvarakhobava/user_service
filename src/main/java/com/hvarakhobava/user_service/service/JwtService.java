package com.hvarakhobava.user_service.service;

import com.hvarakhobava.user_service.dto.JwtTokenConfig;
import com.hvarakhobava.user_service.model.User;

import java.time.temporal.TemporalAmount;
import java.util.Map;

public interface JwtService<C> {
    String issueToken(JwtTokenConfig tokenConfig);

    C verifyAndParse(String token);
}
