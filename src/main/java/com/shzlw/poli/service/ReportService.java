package com.shzlw.poli.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.shzlw.poli.dao.ReportDao;
import com.shzlw.poli.model.Report;
import com.shzlw.poli.model.User;
import com.shzlw.poli.util.Constants;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportService.class);

    /**
     * Key: User id
     * Value: Report
     */
    private static final Cache<Long, List<Report>> USER_REPORT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Autowired
    ReportDao reportDao;

    @Autowired
    private Tracer tracer;

    public List<Report> getReportsByUser(User user) {
        Span span = tracer.spanBuilder("getReportsByUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            if (StringUtils.isEmpty(user)) {
                span.addEvent("Empty user");
                return Collections.emptyList();
            }

            try {
                List<Report> reports = USER_REPORT_CACHE.get(user.getId(), () -> {
                    Span cacheSpan = tracer.spanBuilder("loadReportsFromDatabase").startSpan();
                    try (Scope cacheScope = cacheSpan.makeCurrent()) {
                        List<Report> reportList;
                        if (Constants.SYS_ROLE_VIEWER.equals(user.getSysRole())) {
                            reportList = reportDao.findByViewer(user.getId());
                        } else {
                            reportList = reportDao.findAll();
                        }
                        cacheSpan.addEvent("Reports loaded from database");
                        return reportList;
                    } finally {
                        cacheSpan.end();
                    }
                });
                span.addEvent("Reports retrieved from cache");
                return reports;
            } catch (ExecutionException | CacheLoader.InvalidCacheLoadException e) {
                span.addEvent("Cache load exception");
                return Collections.emptyList();
            }
        } finally {
            span.end();
        }
    }

    public void invalidateCache(long userId) {
        Span span = tracer.spanBuilder("invalidateCache").startSpan();
        try (Scope scope = span.makeCurrent()) {
            USER_REPORT_CACHE.invalidate(userId);
            span.addEvent("Cache invalidated for userId: " + userId);
        } finally {
            span.end();
        }
    }
}
