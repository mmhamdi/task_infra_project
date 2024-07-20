package com.shzlw.poli.config;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OpenTelemetryConfiguration {

    @Bean
    public OpenTelemetrySdk getOpenTelemetry (){
        Resource serviceNameResource =
                Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "poli"));
        OpenTelemetrySdkBuilder openTelemetrySdkBuilder = OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .addSpanProcessor(SimpleSpanProcessor.create(getJaegerExporter()))
                                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                                .setResource(Resource.getDefault().merge(serviceNameResource))
                                .build())
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
        return openTelemetrySdkBuilder.buildAndRegisterGlobal();
    }

    @Bean

	public JaegerGrpcSpanExporter getJaegerExporter(){
    		String jaegerEndpoint = "http://jaeger:14250";
    	return JaegerGrpcSpanExporter.builder()
            .setEndpoint(jaegerEndpoint)
            .setTimeout(30, TimeUnit.SECONDS)
            .build();
}

    @Bean
    public Tracer getTracer(){
        return getOpenTelemetry().getTracer("poli");
    }

    @Bean
    public MeterProvider getMeterProvider(){
        MetricReaderFactory prometheusReaderFactory =
                PrometheusHttpServer.builder().setPort(9091).newMetricReaderFactory();

        return SdkMeterProvider.builder().registerMetricReader(prometheusReaderFactory).build();
    }

    @Bean
    public LoggingSpanExporter getLoggingExporter() {
        return LoggingSpanExporter.create();
    }
}
