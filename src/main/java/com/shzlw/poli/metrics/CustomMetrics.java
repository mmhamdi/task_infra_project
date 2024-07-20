package com.shzlw.poli.metrics;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CustomMetrics {

    private final MeterProvider meterProvider;
    private LongCounter requestCounter;
    private LongCounter errorCounter;
    private LongCounter updateCounter;

    @Autowired
    public CustomMetrics(MeterProvider meterProvider) {
        this.meterProvider = meterProvider;
    }

    @PostConstruct
    public void init() {
        Meter meter = meterProvider.get("poli");

        // Initialize metrics
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
    }

    public LongCounter getRequestCounter() {
        return requestCounter;
    }

    public LongCounter getErrorCounter() {
        return errorCounter;
    }

    public LongCounter getUpdateCounter() {
        return updateCounter;
    }
}
