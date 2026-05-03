package com.linkflow.api.monitoring.service;

import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MonitoringService {

    private final UrlMappingRepository urlMappingRepository;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public MonitoringService(
            UrlMappingRepository urlMappingRepository,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 返回总体健康状态
     */
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "checked_at", OffsetDateTime.now(),
                "services", services()
        );
    }

    /**
     * 返回所有依赖状态
     */
    public Map<String, Object> services() {
        return Map.of(
                "api", Map.of("status", "UP"),
                "postgres", postgresStatus(),
                "redis", redis(),
                "kafka", kafka(),
                "rabbitmq", rabbitmq(),
                "flink", Map.of("status", "not_configured")
        );
    }

    /**
     * 检查 Redis
     */
    public Map<String, Object> redis() {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Map.of("status", "not_configured");
        }
        try {
            var connection = redisTemplate.getConnectionFactory().getConnection();
            String pong;
            try {
                pong = connection.ping();
            } finally {
                connection.close();
            }
            return Map.of("status", "PONG".equalsIgnoreCase(pong) ? "UP" : "UNKNOWN", "ping", pong);
        } catch (Exception ex) {
            return Map.of("status", "DOWN", "error", ex.getClass().getSimpleName());
        }
    }

    /**
     * Kafka 当前未启用
     */
    public Map<String, Object> kafka() {
        return Map.of("status", "not_configured", "reason", "Kafka client dependency is not enabled");
    }

    /**
     * RabbitMQ 当前未启用
     */
    public Map<String, Object> rabbitmq() {
        return Map.of("status", "not_configured", "reason", "RabbitMQ client dependency is not enabled");
    }

    /**
     * Flink 当前未启用
     */
    public Map<String, Object> flinkJobs() {
        return Map.of("status", "not_configured", "jobs", List.of());
    }

    /**
     * 指标时间序列占位
     */
    public List<Map<String, Object>> metricTimeseries(String metric, String window) {
        return List.of();
    }

    private Map<String, Object> postgresStatus() {
        try {
            urlMappingRepository.count();
            return Map.of("status", "UP");
        } catch (Exception ex) {
            return Map.of("status", "DOWN", "error", ex.getClass().getSimpleName());
        }
    }
}
