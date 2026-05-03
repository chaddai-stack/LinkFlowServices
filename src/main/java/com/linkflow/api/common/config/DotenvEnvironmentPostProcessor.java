package com.linkflow.api.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "localDotenv";

    /**
     * 加载本地 .env
     *
     * 只在环境中不存在同名 key 时注入，确保 CI/CD 或系统环境变量优先级更高。
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenv = Path.of(".env");
        if (!Files.isRegularFile(dotenv)) {
            return;
        }

        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(dotenv, StandardCharsets.UTF_8)) {
                parseLine(line).ifPresent(entry -> {
                    if (!environment.containsProperty(entry.name())) {
                        values.put(entry.name(), entry.value());
                    }
                });
            }
        } catch (IOException ex) {
            return;
        }

        if (!values.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private java.util.Optional<Entry> parseLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isBlank() || trimmed.startsWith("#")) {
            return java.util.Optional.empty();
        }

        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
            return java.util.Optional.empty();
        }

        String name = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        if (name.isBlank()) {
            return java.util.Optional.empty();
        }

        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        return java.util.Optional.of(new Entry(name, value));
    }

    private record Entry(String name, String value) {
    }
}
