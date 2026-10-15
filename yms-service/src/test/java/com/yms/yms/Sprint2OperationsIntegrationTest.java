package com.yms.yms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yms.yms.domain.partner.entity.Partner;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import com.yms.yms.domain.service.entity.ServiceEntity;
import com.yms.yms.domain.service.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class Sprint2OperationsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PartnerRepository partnerRepository;
    @Autowired ServiceRepository serviceRepository;

    @Test
    void completesRegistrationQrGateServiceAndYardFlow() throws Exception {
        Partner partner = new Partner();
        partner.setCode("SPRINT2-" + UUID.randomUUID());
        partner.setName("Integration Partner");
        partner.setCountryCode("VN");
        partner.setPartnerType("A");
        Partner savedPartner = partnerRepository.save(partner);

        ServiceEntity service = new ServiceEntity();
        service.setCode("S2-" + UUID.randomUUID());
        service.setName("Loading");
        service.setServiceType("HANDLING");
        service.setUnit("HOUR");
        service.setHourlyRate(new BigDecimal("360.0000"));
        ServiceEntity savedService = serviceRepository.save(service);

        String yardJson = mockMvc.perform(post("/api/yards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new YardPayload(
                                "Y-" + UUID.randomUUID(), "Integration Yard", "Test address", 1, "ACTIVE"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID yardId = UUID.fromString(objectMapper.readTree(yardJson).path("data").path("id").asText());
        mockMvc.perform(get("/api/yards")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));

        Instant arrival = Instant.now().plusSeconds(5 * 60 * 60);
        String registrationRequest = objectMapper.writeValueAsString(new RegistrationPayload(
                savedPartner.getId(), " 51c 12345 ", arrival, "Test Driver", List.of(savedService.getId())));
        String registrationJson = mockMvc.perform(post("/api/vehicles/pre-notifications")
                        .contentType(MediaType.APPLICATION_JSON).content(registrationRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode registration = objectMapper.readTree(registrationJson).path("data");
        String notificationId = registration.path("id").asText();
        String vehicleId = registration.path("vehicleId").asText();
        assertTrue(!notificationId.isBlank());

        String qrJson = mockMvc.perform(post("/api/vehicles/pre-notifications/{id}/qr", notificationId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String qrToken = objectMapper.readTree(qrJson).path("data").path("token").asText();
        assertTrue(!qrToken.isBlank());

        mockMvc.perform(post("/api/yard-logs/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScanPayload(qrToken + "tampered"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/yard-logs/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScanPayload(qrToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plateNumber").value("51C12345"));

        String checkInPayload = objectMapper.writeValueAsString(new CheckInPayload(qrToken, yardId));
        mockMvc.perform(post("/api/yard-logs/check-in")
                        .contentType(MediaType.APPLICATION_JSON).content(checkInPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(vehicleId));
        mockMvc.perform(post("/api/yard-logs/check-in")
                        .contentType(MediaType.APPLICATION_JSON).content(checkInPayload))
                .andExpect(status().isGone());
        mockMvc.perform(get("/api/vehicles/pre-notifications/in-yard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post("/api/yards/{yardId}/assignments", yardId)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new ScanPayload(qrToken))))
                .andExpect(status().isGone());
        mockMvc.perform(post("/api/vehicles/pre-notifications/{id}/qr", notificationId))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/yards/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].occupancy").value(1))
                .andExpect(jsonPath("$.data[0].alert").value(true));
        mockMvc.perform(get("/api/yards/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        String ordersJson = mockMvc.perform(get("/api/service-orders").param("vehicleId", vehicleId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(ordersJson).path("data").get(0).path("id").asText();
        mockMvc.perform(post("/api/service-orders/{id}/start", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        Thread.sleep(40);
        mockMvc.perform(post("/api/service-orders/{id}/end", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/yard-logs/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckOutPayload(UUID.fromString(notificationId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.yardLog.durationSeconds").exists());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/yards/{id}", yardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new YardPayload(
                                "Y-unused", "Updated Yard", "Updated address", 2, "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(2));

        Instant secondArrival = Instant.now().plusSeconds(5 * 60 * 60);
        String secondRegistrationJson = mockMvc.perform(post("/api/vehicles/pre-notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationPayload(savedPartner.getId(),
                                "60A55555", secondArrival, "Second Driver", List.of()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondNotificationId = objectMapper.readTree(secondRegistrationJson).path("data").path("id").asText();
        String secondQr = objectMapper.readTree(mockMvc.perform(
                        post("/api/vehicles/pre-notifications/{id}/qr", secondNotificationId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("data").path("token").asText();
        mockMvc.perform(post("/api/yards/{yardId}/assignments", yardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScanPayload(secondQr))))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/yards/{id}", yardId))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/yard-logs/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckOutPayload(UUID.fromString(secondNotificationId)))))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/yards/{id}", yardId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/yard-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post("/api/vehicles/pre-notifications")
                        .contentType(MediaType.APPLICATION_JSON).content(registrationRequest))
                .andExpect(status().isConflict());
    }

    private record RegistrationPayload(UUID partnerAId, String plateNumber, Instant plannedArrival,
                                       String driverName, java.util.List<UUID> serviceIds) {
    }

    private record ScanPayload(String token) {
    }

    private record CheckInPayload(String token, UUID yardId) {
    }

    private record CheckOutPayload(UUID preNotificationId) {
    }

    private record YardPayload(String code, String name, String address, int capacity, String status) {
    }
}
