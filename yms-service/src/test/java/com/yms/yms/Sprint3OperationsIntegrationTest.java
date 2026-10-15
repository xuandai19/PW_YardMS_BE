package com.yms.yms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yms.yms.domain.partner.entity.Partner;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.entity.Vehicle;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class Sprint3OperationsIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PartnerRepository partnerRepository;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired TripRepository tripRepository;

    @Test
    void completesGpsGoodsAndDriverConfirmationFlows() throws Exception {
        Trip trip = createTrip();
        UUID tripId = trip.getId();

        mockMvc.perform(get("/api/tracking/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(post("/api/tracking/trips/{tripId}/geofences", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Loading yard","latitude":10.0,"longitude":106.0,"radiusM":200}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.radiusM").value(200));

        String gps = objectMapper.writeValueAsString(new GpsPayload(tripId, Instant.now(),
                new BigDecimal("10.001"), new BigDecimal("106.001")));
        mockMvc.perform(post("/api/tracking/gps").contentType(MediaType.APPLICATION_JSON).content(gps))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offRoute").value(false));
        String outsideGps = objectMapper.writeValueAsString(new GpsPayload(tripId, Instant.now(),
                new BigDecimal("11.0"), new BigDecimal("107.0")));
        mockMvc.perform(post("/api/tracking/gps").contentType(MediaType.APPLICATION_JSON).content(outsideGps))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offRoute").value(true));
        mockMvc.perform(get("/api/tracking/trips/{tripId}/history", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/api/tracking/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        String batch = """
                {"tripId":"%s","items":[
                  {"itemCode":"PALLET-1","description":"Pallet","quantity":10,"unit":"box"},
                  {"itemCode":"PALLET-2","description":"Parts","quantity":4,"unit":"box"}
                ]}
                """.formatted(tripId);
        String batchJson = mockMvc.perform(post("/api/goods/batch")
                        .contentType(MediaType.APPLICATION_JSON).content(batch))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode firstGoods = objectMapper.readTree(batchJson).path("data").get(0).path("goods");
        String goodsId = firstGoods.path("id").asText();
        String qrValue = firstGoods.path("qrValue").asText();
        assertTrue(!qrValue.isBlank());
        mockMvc.perform(get("/api/goods").param("tripId", tripId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/api/goods/{goodsId}/qr", goodsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageBase64").isNotEmpty());
        mockMvc.perform(post("/api/goods/{goodsId}/reconcile", goodsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReconcilePayload(qrValue, BigDecimal.valueOf(12)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mismatch").value(true));
        mockMvc.perform(get("/api/goods/discrepancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post("/api/goods/{goodsId}/reconcile", goodsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReconcilePayload(qrValue, BigDecimal.TEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mismatch").value(false));

        String confirmationJson = mockMvc.perform(post("/api/confirmations/trips/{tripId}", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmationType":"DEPARTURE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.dueAt").exists())
                .andReturn().getResponse().getContentAsString();
        String confirmationId = objectMapper.readTree(confirmationJson).path("data").path("id").asText();
        mockMvc.perform(get("/api/confirmations").param("tripId", tripId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post("/api/confirmations/{id}/confirm", confirmationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        MockMultipartFile signature = new MockMultipartFile("file", "signature.png",
                "image/png", new byte[]{1, 2, 3, 4});
        mockMvc.perform(multipart("/api/confirmations/{id}/signature", confirmationId).file(signature))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signatureUrl").isNotEmpty());
        mockMvc.perform(get("/api/confirmations/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private Trip createTrip() {
        Partner partner = new Partner();
        partner.setCode("S3-" + UUID.randomUUID());
        partner.setName("Sprint 3 integration partner");
        partner.setCountryCode("VN");
        partner.setPartnerType("A");
        partner = partnerRepository.save(partner);

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("S3-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setPartnerAId(partner.getId());
        vehicle.setDriverName("Integration driver");
        vehicle = vehicleRepository.save(vehicle);

        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setVehicleId(vehicle.getId());
        trip.setOrigin("Origin");
        trip.setDestination("Destination");
        trip.setPlannedAt(Instant.now());
        return tripRepository.save(trip);
    }

    private record GpsPayload(UUID tripId, Instant recordedAt, BigDecimal latitude, BigDecimal longitude) {
    }

    private record ReconcilePayload(String qrValue, BigDecimal scannedQuantity) {
    }
}
