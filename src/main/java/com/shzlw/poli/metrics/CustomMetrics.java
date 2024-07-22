package com.shzlw.poli.metrics;

import io.opentelemetry.api.metrics.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CustomMetrics {

    private final MeterProvider meterProvider;

    // Counters
    private LongCounter requestCounter;
    private LongCounter errorCounter;
    private LongCounter updateCounter;
    private LongCounter dbQueryCounter;
    private LongCounter dbFailedQueryCounter;
    private LongCounter serviceMethodCounter;
    private LongCounter serviceOperationSuccessCounter;
    private LongCounter serviceOperationFailureCounter;
    private LongCounter cacheHitCounter;
    private LongCounter cacheMissCounter;

    // UpDownCounters (used as gauges)
    private LongUpDownCounter activeSessionsCounter;

    // Histograms (removed duration related ones)
    private DoubleHistogram requestSizeHistogram;

    @Autowired
    public CustomMetrics(MeterProvider meterProvider) {
        this.meterProvider = meterProvider;
    }

    @PostConstruct
    public void init() {
        Meter meter = meterProvider.get("poli");

        // Initialize counters
        requestCounter = meter.counterBuilder("http_requests_total")
                .setDescription("Total number of HTTP requests")
                .setUnit("requests")
                .build();

        errorCounter = meter.counterBuilder("http_requests_errors_total")
                .setDescription("Total number of HTTP request errors")
                .setUnit("requests")
                .build();

        updateCounter = meter.counterBuilder("reports_updates_total")
                .setDescription("Total number of reports updates")
                .setUnit("updates")
                .build();

        dbQueryCounter = meter.counterBuilder("db_queries_total")
                .setDescription("Total number of database queries executed")
                .setUnit("queries")
                .build();

        dbFailedQueryCounter = meter.counterBuilder("db_failed_queries_total")
                .setDescription("Total number of failed database queries")
                .setUnit("queries")
                .build();

        serviceMethodCounter = meter.counterBuilder("service_method_calls_total")
                .setDescription("Total number of times a specific service method is called")
                .setUnit("calls")
                .build();

        serviceOperationSuccessCounter = meter.counterBuilder("service_operation_success_total")
                .setDescription("Total number of successful service operations")
                .setUnit("operations")
                .build();

        serviceOperationFailureCounter = meter.counterBuilder("service_operation_failure_total")
                .setDescription("Total number of failed service operations")
                .setUnit("operations")
                .build();

        // Initialize UpDownCounters (used as gauges)
        activeSessionsCounter = meter.upDownCounterBuilder("active_sessions")
                .setDescription("Number of active user sessions")
                .setUnit("sessions")
                .build();

        // Initialize histograms
        requestSizeHistogram = meter.histogramBuilder("http_request_size_bytes")
                .setDescription("Histogram of HTTP request sizes")
                .setUnit("bytes")
                .build();

        // Initialize cache metrics
        cacheHitCounter = meter.counterBuilder("cache_hits_total")
                .setDescription("Total number of cache hits")
                .setUnit("hits")
                .build();

        cacheMissCounter = meter.counterBuilder("cache_misses_total")
                .setDescription("Total number of cache misses")
                .setUnit("misses")
                .build();
    }

    // Getter methods for counters and histograms
    public LongCounter getRequestCounter() {
        return requestCounter;
    }

    public LongCounter getErrorCounter() {
        return errorCounter;
    }

    public LongCounter getUpdateCounter() {
        return updateCounter;
    }

    public LongCounter getDbQueryCounter() {
        return dbQueryCounter;
    }

    public LongCounter getDbFailedQueryCounter() {
        return dbFailedQueryCounter;
    }

    public LongCounter getServiceMethodCounter() {
        return serviceMethodCounter;
    }

    public LongCounter getServiceOperationSuccessCounter() {
        return serviceOperationSuccessCounter;
    }

    public LongCounter getServiceOperationFailureCounter() {
        return serviceOperationFailureCounter;
    }

    public LongCounter getCacheHitCounter() {
        return cacheHitCounter;
    }

    public LongCounter getCacheMissCounter() {
        return cacheMissCounter;
    }

    public LongUpDownCounter getActiveSessionsCounter() {
        return activeSessionsCounter;
    }

    public DoubleHistogram getRequestSizeHistogram() {
        return requestSizeHistogram;
    }

    // Removed methods for recording duration metrics
}
