package com.shzlw.poli.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.shzlw.poli.dao.UserDao;
import com.shzlw.poli.model.User;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    @Autowired
    Tracer tracer ;

    /**
     * Key: Session key
     * Value: User
     */
    private static final Cache<String, User> SESSION_USER_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    /**
     * Key: Api key
     * Value: User
     */
    private static final Cache<String, User> API_KEY_USER_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Autowired
    UserDao userDao;

    public User getUserBySessionKey(String sessionKey) {
        if (StringUtils.isEmpty(sessionKey)) {
            return null;
        }

        Span span = tracer.spanBuilder("getUserBySessionKey").startSpan();
        try {
            return SESSION_USER_CACHE.get(sessionKey, () -> {
                Span cacheSpan = tracer.spanBuilder("cacheLoader").startSpan();
                try {
                    User u = userDao.findBySessionKey(sessionKey);
                    u.setUserAttributes(userDao.findUserAttributes(u.getId()));
                    return u;
                } finally {
                    cacheSpan.end();
                }
            });
        } catch (ExecutionException | CacheLoader.InvalidCacheLoadException e) {
            LOGGER.error("Failed to load user by session key", e);
            return null;
        } finally {
            span.end();
        }
    }

    public User getUserByApiKey(String apiKey) {
        if (StringUtils.isEmpty(apiKey)) {
            return null;
        }

        Span span = tracer.spanBuilder("getUserByApiKey").startSpan();
        try {
            return API_KEY_USER_CACHE.get(apiKey, () -> {
                Span cacheSpan = tracer.spanBuilder("cacheLoader").startSpan();
                try {
                    User u = userDao.findByApiKey(apiKey);
                    u.setUserAttributes(userDao.findUserAttributes(u.getId()));
                    return u;
                } finally {
                    cacheSpan.end();
                }
            });
        } catch (ExecutionException | CacheLoader.InvalidCacheLoadException e) {
            LOGGER.error("Failed to load user by API key", e);
            return null;
        } finally {
            span.end();
        }
    }

    public void newOrUpdateUser(User user, String oldSessionKey, String newSessionKey) {
        Span span = tracer.spanBuilder("newOrUpdateUser").startSpan();
        try {
            invalidateSessionUserCache(oldSessionKey);
            SESSION_USER_CACHE.put(newSessionKey, user);
        } finally {
            span.end();
        }
    }

    public void invalidateSessionUserCache(String sessionKey) {
        Span span = tracer.spanBuilder("invalidateSessionUserCache").startSpan();
        try {
            if (sessionKey != null) {
                SESSION_USER_CACHE.invalidate(sessionKey);
            }
        } finally {
            span.end();
        }
    }

    public void invalidateApiKeyUserCache(String apiKey) {
        Span span = tracer.spanBuilder("invalidateApiKeyUserCache").startSpan();
        try {
            if (apiKey != null) {
                API_KEY_USER_CACHE.invalidate(apiKey);
            }
        } finally {
            span.end();
        }
    }
}
