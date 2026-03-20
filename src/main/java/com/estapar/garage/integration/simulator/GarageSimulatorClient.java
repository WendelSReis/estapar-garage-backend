package com.estapar.garage.integration.simulator;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GarageSimulatorClient {

    private final RestClient restClient;

    public GarageSimulatorClient(RestClient simulatorRestClient) {
        this.restClient = simulatorRestClient;
    }

    public GarageSimulatorResponse getGarageConfiguration() {
        return restClient.get()
                .uri("/garage")
                .retrieve()
                .body(GarageSimulatorResponse.class);
    }
}
