# Observability Stack and Service Instrumentation

## Overview

This project involves instrumenting a Java-based service with OpenTelemetry and setting up a compatible monitoring stack. The chosen stack includes Grafana, Jaeger, and Prometheus. The goal is to enable comprehensive tracing, metrics collection, and logging for enhanced observability of the service.

## Table of Contents

1. [Architecture](#Architecture)
2. [Infrastructure Setup](#infrastructure-setup)
3. [Service Instrumentation](#service-instrumentation)
4. [Metrics](#metrics)
5. [Dashboards and Alerts](#dashboards-and-alerts)
6. [Optional: Node Exporter](#optional-node-exporter)
7. [Conclusion](#conclusion)

## Architecture
## Infrastructure Setup

### Components

- **Grafana**: For visualizing metrics and logs.
- **Jaeger**: For tracing and monitoring request flows.
- **Prometheus**: For collecting and querying metrics.
- **Loki**: For centralized logging (integrated with Grafana).

### Docker Compose Configuration

The observability stack is configured using Docker Compose. Persistent data storage is set up using bind mounts to ensure data durability across container restarts.

#### Docker Compose File

```yaml
version: '3'

services:
  jaeger:
    image: jaegertracing/all-in-one:1.32
    ports:
      - "5775:5775"
      - "5778:5778"
      - "14250:14250"
      - "14268:14268"
      - "14250:14250"
      - "16686:16686"
    volumes:
      - jaeger-data:/var/lib/jaeger

  prometheus:
    image: prom/prometheus:2.38
    ports:
      - "9090:9090"
    volumes:
      - prometheus-config:/etc/prometheus
      - prometheus-data:/prometheus

  grafana:
    image: grafana/grafana:8.5
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana

  loki:
    image: grafana/loki:2.8
    ports:
      - "3100:3100"
    volumes:
      - loki-data:/loki

volumes:
  jaeger-data:
  prometheus-config:
  prometheus-data:
  grafana-data:
  loki-data:
