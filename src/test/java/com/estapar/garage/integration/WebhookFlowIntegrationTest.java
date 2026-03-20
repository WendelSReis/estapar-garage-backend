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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:mysql:8.0.36:///garage_db",
        "spring.datasource.username=test",
        "spring.datasource.password=test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ParkingSessionRepository parkingSessionRepository;

    @BeforeEach
    void setup() {
        parkingSessionRepository.deleteAll();
        parkingSpotRepository.deleteAll();
        sectorRepository.deleteAll();

        SectorEntity sector = new SectorEntity();
        sector.setSectorCode("A");
        sector.setBasePrice(new BigDecimal("10.00"));
        sector.setMaxCapacity(2);
        sectorRepository.save(sector);

        ParkingSpotEntity spot1 = new ParkingSpotEntity();
        spot1.setId(1L);
        spot1.setSector(sector);
        spot1.setLat(new BigDecimal("-23.561684"));
        spot1.setLng(new BigDecimal("-46.655981"));
        spot1.setStatus(SpotStatus.FREE);
        parkingSpotRepository.save(spot1);

        ParkingSpotEntity spot2 = new ParkingSpotEntity();
        spot2.setId(2L);
        spot2.setSector(sector);
        spot2.setLat(new BigDecimal("-23.561685"));
        spot2.setLng(new BigDecimal("-46.655982"));
        spot2.setStatus(SpotStatus.FREE);
        parkingSpotRepository.save(spot2);
    }

    @Test
    void shouldProcessEntryParkedExitAndReturnRevenue() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"license_plate\": \"ZUL0001\",
                                  \"entry_time\": \"2025-01-01T12:00:00.000Z\",
                                  \"event_type\": \"ENTRY\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ENTRY processado"));

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"license_plate\": \"ZUL0001\",
                                  \"lat\": -23.561684,
                                  \"lng\": -46.655981,
                                  \"event_type\": \"PARKED\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("PARKED processado"));

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"license_plate\": \"ZUL0001\",
                                  \"exit_time\": \"2025-01-01T13:01:00.000Z\",
                                  \"event_type\": \"EXIT\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("EXIT processado"));

        mockMvc.perform(get("/revenue")
                        .queryParam("date", "2025-01-01")
                        .queryParam("sector", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(9.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void shouldRejectParkingWhenSpotIsAlreadyOccupied() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"license_plate\": \"AAA0001\",
                                  \"entry_time\": \"2025-01-01T12:00:00.000Z\",
                                  \"event_type\": \"ENTRY\"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"license_plate\": \"AAA0001\",
                                  \"lat\": -23.561684,
                                  \"lng\": -46.655981,
                                  \"event_type\": \"PARKED\"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"license_plate\": \"BBB0001\",
                                  \"entry_time\": \"2025-01-01T12:05:00.000Z\",
                                  \"event_type\": \"ENTRY\"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"license_plate\": \"BBB0001\",
                                  \"lat\": -23.561684,
                                  \"lng\": -46.655981,
                                  \"event_type\": \"PARKED\"
                                }
                                """))
                .andExpect(status().isConflict());
    }
}
