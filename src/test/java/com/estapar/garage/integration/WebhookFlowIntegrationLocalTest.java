package com.estapar.garage.integration;

import com.estapar.garage.domain.enums.SpotStatus;
import com.estapar.garage.persistence.entity.ParkingSpotEntity;
import com.estapar.garage.persistence.entity.SectorEntity;
import com.estapar.garage.persistence.repository.ParkingSessionRepository;
import com.estapar.garage.persistence.repository.ParkingSpotRepository;
import com.estapar.garage.persistence.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-local")
class WebhookFlowIntegrationLocalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ParkingSessionRepository parkingSessionRepository;

    /**
     * Evita que o runner de startup tente sincronizar com o simulador
     * durante os testes locais.
     */
    @MockBean
    private com.estapar.garage.domain.service.GarageSynchronizationService garageSynchronizationService;

    @BeforeEach
    void setUp() {
        doNothing().when(garageSynchronizationService).synchronizeGarage();

        parkingSessionRepository.deleteAll();
        parkingSpotRepository.deleteAll();
        sectorRepository.deleteAll();

        seedGarage();
    }

    private void seedGarage() {
        SectorEntity sectorA = new SectorEntity();
        sectorA.setSectorCode("A");
        sectorA.setBasePrice(new BigDecimal("10.00"));
        sectorA.setMaxCapacity(2);
        sectorA = sectorRepository.saveAndFlush(sectorA);

        SectorEntity sectorB = new SectorEntity();
        sectorB.setSectorCode("B");
        sectorB.setBasePrice(new BigDecimal("20.00"));
        sectorB.setMaxCapacity(1);
        sectorB = sectorRepository.saveAndFlush(sectorB);

        ParkingSpotEntity spotA1 = new ParkingSpotEntity();
        spotA1.setId(1L);
        spotA1.setSector(sectorA);
        spotA1.setLat(new BigDecimal("-23.561684"));
        spotA1.setLng(new BigDecimal("-46.655981"));
        spotA1.setStatus(SpotStatus.FREE);

        ParkingSpotEntity spotA2 = new ParkingSpotEntity();
        spotA2.setId(2L);
        spotA2.setSector(sectorA);
        spotA2.setLat(new BigDecimal("-23.561700"));
        spotA2.setLng(new BigDecimal("-46.655990"));
        spotA2.setStatus(SpotStatus.FREE);

        ParkingSpotEntity spotB1 = new ParkingSpotEntity();
        spotB1.setId(3L);
        spotB1.setSector(sectorB);
        spotB1.setLat(new BigDecimal("-23.562000"));
        spotB1.setLng(new BigDecimal("-46.656500"));
        spotB1.setStatus(SpotStatus.FREE);

        parkingSpotRepository.save(spotA1);
        parkingSpotRepository.save(spotA2);
        parkingSpotRepository.save(spotB1);
    }

    @Test
    void deveProcessarFluxoCompletoEntryParkedExitEConsultarRevenue() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ZUL0001",
                                  "entry_time": "2025-01-01T12:00:00.000Z",
                                  "event_type": "ENTRY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ZUL0001",
                                  "lat": -23.561684,
                                  "lng": -46.655981,
                                  "event_type": "PARKED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ZUL0001",
                                  "exit_time": "2025-01-01T14:10:00.000Z",
                                  "event_type": "EXIT"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/revenue")
                        .param("date", "2025-01-01")
                        .param("sector", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("BRL")))
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void deveCobrarZeroQuandoTempoForMenorOuIgualA30Minutos() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "FREE0001",
                                  "entry_time": "2025-01-01T12:00:00.000Z",
                                  "event_type": "ENTRY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "FREE0001",
                                  "lat": -23.561684,
                                  "lng": -46.655981,
                                  "event_type": "PARKED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "FREE0001",
                                  "exit_time": "2025-01-01T12:20:00.000Z",
                                  "event_type": "EXIT"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/revenue")
                        .param("date", "2025-01-01")
                        .param("sector", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(0.0)));
    }

    @Test
    void deveAplicarArredondamentoParaCimaNoTempoExcedente() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ROUND001",
                                  "entry_time": "2025-01-01T12:00:00.000Z",
                                  "event_type": "ENTRY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ROUND001",
                                  "lat": -23.561684,
                                  "lng": -46.655981,
                                  "event_type": "PARKED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ROUND001",
                                  "exit_time": "2025-01-01T13:31:00.000Z",
                                  "event_type": "EXIT"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/revenue")
                        .param("date", "2025-01-01")
                        .param("sector", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", closeTo(18.0, 0.001)));
    }

    @Test
    void deveBloquearEntradaQuandoGaragemInteiraEstiverLotada() throws Exception {
        entrar("CAR001", "2025-01-01T12:00:00.000Z");
        entrar("CAR002", "2025-01-01T12:01:00.000Z");
        entrar("CAR003", "2025-01-01T12:02:00.000Z");

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "CAR004",
                                  "entry_time": "2025-01-01T12:03:00.000Z",
                                  "event_type": "ENTRY"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void deveUsarOutroSetorQuandoPrimeiroSetorEstiverLotado() throws Exception {
        entrar("A001", "2025-01-01T12:00:00.000Z");
        entrar("A002", "2025-01-01T12:01:00.000Z");

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "B001",
                                  "entry_time": "2025-01-01T12:02:00.000Z",
                                  "event_type": "ENTRY"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deveFalharQuandoParkedNaoBaterComVagaReservada() throws Exception {
        entrar("FAIL001", "2025-01-01T12:00:00.000Z");

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "FAIL001",
                                  "lat": -99.999999,
                                  "lng": -99.999999,
                                  "event_type": "PARKED"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    private void entrar(String plate, String entryTime) throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "%s",
                                  "entry_time": "%s",
                                  "event_type": "ENTRY"
                                }
                                """.formatted(plate, entryTime)))
                .andExpect(status().isOk());
    }
}