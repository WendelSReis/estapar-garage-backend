package com.estapar.garage.bootstrap;

import com.estapar.garage.config.SimulatorProperties;
import com.estapar.garage.domain.service.GarageSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GarageInitializationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GarageInitializationRunner.class);

    private final SimulatorProperties simulatorProperties;
    private final GarageSynchronizationService garageSynchronizationService;

    public GarageInitializationRunner(SimulatorProperties simulatorProperties,
                                      GarageSynchronizationService garageSynchronizationService) {
        this.simulatorProperties = simulatorProperties;
        this.garageSynchronizationService = garageSynchronizationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!simulatorProperties.enabled()) {
            log.info("Sincronização com simulador desabilitada");
            return;
        }

        garageSynchronizationService.synchronizeGarage();
        log.info("Configuração da garagem sincronizada com sucesso");
    }
}
