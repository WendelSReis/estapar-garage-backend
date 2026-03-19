package com.estapar.garage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(boolean enabled, String baseUrl) {
}
