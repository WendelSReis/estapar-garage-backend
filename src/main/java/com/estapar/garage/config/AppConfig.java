package com.estapar.garage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SimulatorProperties.class)
public class AppConfig {

    @Bean
    RestClient simulatorRestClient(RestClient.Builder builder, SimulatorProperties properties) {
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
