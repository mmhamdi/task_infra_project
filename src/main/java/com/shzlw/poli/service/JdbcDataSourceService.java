package com.shzlw.poli.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.shzlw.poli.config.AppProperties;
import com.shzlw.poli.dao.JdbcDataSourceDao;
import com.shzlw.poli.model.JdbcDataSource;
import com.zaxxer.hikari.HikariDataSource;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class JdbcDataSourceService {
    @Autowired
    Tracer tracer ;
    // 10 seconds
    private static final int LEAK_DETECTION_THRESHOLD = 10000;

    /**
     * Key: JdbcDataSource id
     * Value: HikariDataSource
     */
    private static final Cache<Long, HikariDataSource> DATA_SOURCE_CACHE = CacheBuilder.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .removalListener((RemovalListener<Long, HikariDataSource>) removal -> {
        HikariDataSource ds = removal.getValue();
        ds.close();
    })
    .build();

@Autowired
JdbcDataSourceDao jdbcDataSourceDao;

@Autowired
AppProperties appProperties;

@PostConstruct
public void init() {
}

@PreDestroy
public void shutdown() {
for (HikariDataSource hiDs : DATA_SOURCE_CACHE.asMap().values()) {
    hiDs.close();
}
}

    public void removeFromCache(long dataSourceId) {
        Span span = tracer.spanBuilder("removeFromCache").startSpan();
        try (Scope scope = span.makeCurrent()) {
            DATA_SOURCE_CACHE.invalidate(dataSourceId);
        } finally {
            span.end();
        }
    }

    public DataSource getDataSource(long dataSourceId) {
        Span span = tracer.spanBuilder("getDataSource").startSpan();
        try (Scope scope = span.makeCurrent()) {
            if (dataSourceId == 0) {
                return null;
            }

            try {
                DataSource hiDs = DATA_SOURCE_CACHE.get(dataSourceId, () -> {
                    Span cacheLoaderSpan = tracer.spanBuilder("cacheLoader").startSpan();
                    try (Scope loaderScope = cacheLoaderSpan.makeCurrent()) {
                        JdbcDataSource dataSource = jdbcDataSourceDao.findById(dataSourceId);
                        if (dataSource == null) {
                            return null;
                        }
                        HikariDataSource newHiDs = new HikariDataSource();
                        newHiDs.setJdbcUrl(dataSource.getConnectionUrl());
                        newHiDs.setUsername(dataSource.getUsername());
                        newHiDs.setPassword(dataSource.getPassword());
                        if (!StringUtils.isEmpty(dataSource.getDriverClassName())) {
                            newHiDs.setDriverClassName(dataSource.getDriverClassName());
                        }
                        newHiDs.setMaximumPoolSize(appProperties.getDatasourceMaximumPoolSize());
                        newHiDs.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);
                        return newHiDs;
                    } finally {
                        cacheLoaderSpan.end();
                    }
                });
                return hiDs;
            } catch (ExecutionException | CacheLoader.InvalidCacheLoadException e) {
                span.recordException(e);
                return null;
            }
        } finally {
            span.end();
        }
    }
}
