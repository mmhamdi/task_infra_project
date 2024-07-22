package com.shzlw.poli.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.shzlw.poli.dao.ReportDao;
import com.shzlw.poli.model.Report;
import com.shzlw.poli.model.User;
import com.shzlw.poli.util.Constants;
import com.shzlw.poli.metrics.CustomMetrics;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportService.class);
    private static final Cache<Long, List<Report>> USER_REPORT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private Tracer tracer;

    @Autowired
    private CustomMetrics customMetrics;

    public List<Report> getReportsByUser(User user) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("getReportsByUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            customMetrics.getServiceMethodCounter().add(1); // Count method calls
            LOGGER.info("Starting getReportsByUser method for user: {}", user);

            if (user == null) {
                LOGGER.warn("Empty user provided to getReportsByUser");
                span.addEvent("Empty user");
                customMetrics.getErrorCounter().add(1); // Track error count
                return Collections.emptyList();
            }

            try {
                List<Report> reports = USER_REPORT_CACHE.get(user.getId(), () -> {
                    long cacheStartTime = System.nanoTime();
                    Span cacheSpan = tracer.spanBuilder("loadReportsFromDatabase").startSpan();
                    try (Scope cacheScope = cacheSpan.makeCurrent()) {
                        LOGGER.info("Loading reports from database for user: {}", user.getId());
                        List<Report> reportList;
                        if (Constants.SYS_ROLE_VIEWER.equals(user.getSysRole())) {
                            reportList = reportDao.findByViewer(user.getId());
                        } else {
                            reportList = reportDao.findAll();
                        }
                        cacheSpan.addEvent("Reports loaded from database");
                        LOGGER.info("Reports loaded from database for user: {}", user.getId());
                        customMetrics.getCacheMissCounter().add(1); // Track cache misses
                        return reportList;
                    } finally {
                        long cacheDuration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - cacheStartTime);
                        cacheSpan.end();
                    }
                });
                span.addEvent("Reports retrieved from cache");
                LOGGER.info("Reports retrieved from cache for user: {}", user.getId());
                customMetrics.getCacheHitCounter().add(1); // Track cache hits
                return reports;
            } catch (ExecutionException | CacheLoader.InvalidCacheLoadException e) {
                LOGGER.error("Error retrieving reports from cache for user: {}", user.getId(), e);
                span.addEvent("Cache load exception");
                customMetrics.getErrorCounter().add(1); // Track error count
                return Collections.emptyList();
            }
        } finally {
            long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            span.end();
            LOGGER.info("Completed getReportsByUser method for user: {}", user);
        }
    }

    public void invalidateCache(long userId) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("invalidateCache").startSpan();
        try (Scope scope = span.makeCurrent()) {
            customMetrics.getServiceMethodCounter().add(1); // Count method calls
            LOGGER.info("Invalidating cache for userId: {}", userId);
            USER_REPORT_CACHE.invalidate(userId);
            span.addEvent("Cache invalidated for userId: " + userId);
            LOGGER.info("Cache invalidated for userId: {}", userId);
        } finally {
            long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            span.end();
        }
    }

    public void updateReport(Report report) {
        Span span = tracer.spanBuilder("updateReport").startSpan();
        try (Scope scope = span.makeCurrent()) {
            LOGGER.info("Updating report: {}", report);
            reportDao.update(report);
            span.addEvent("Report updated");
            customMetrics.getUpdateCounter().add(1); // Track update count
        } catch (Exception e) {
            LOGGER.error("Error updating report: {}", report, e);
            span.addEvent("Update error");
            customMetrics.getErrorCounter().add(1); // Track error count
        } finally {
            span.end();
        }
    }
}
