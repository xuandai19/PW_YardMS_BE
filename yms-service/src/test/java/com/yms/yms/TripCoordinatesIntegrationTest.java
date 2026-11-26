package com.yms.yms;

import com.yms.yms.domain.partner.entity.Partner;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.entity.Vehicle;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripCoordinatesIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired EntityManager entityManager;
    @Autowired PartnerRepository partnerRepository;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired TripRepository tripRepository;

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "TRACKING_WRITE"})
    void updatesAndPersistsOriginCoordinates() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(patch("/api/trips/{id}/coordinates", trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originLatitude":10.1234567,"originLongitude":106.7654321}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originLatitude").value(10.1234567))
                .andExpect(jsonPath("$.data.originLongitude").value(106.7654321))
                .andExpect(jsonPath("$.data.destinationLatitude").value(20.0))
                .andExpect(jsonPath("$.data.destinationLongitude").value(107.0));

        entityManager.flush();
        entityManager.clear();
        Trip updated = tripRepository.findById(trip.getId()).orElseThrow();
        assertEquals(new BigDecimal("10.1234567"), updated.getOriginLatitude());
        assertEquals(new BigDecimal("106.7654321"), updated.getOriginLongitude());
        assertEquals(new BigDecimal("20.000000"), updated.getDestinationLatitude());
        assertEquals(new BigDecimal("107.000000"), updated.getDestinationLongitude());

        mockMvc.perform(patch("/api/trips/{id}/coordinates", trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationLatitude":21.012345,"destinationLongitude":108.123456}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destinationLatitude").value(21.012345))
                .andExpect(jsonPath("$.data.destinationLongitude").value(108.123456));

        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].originLatitude").value(10.1234567))
                .andExpect(jsonPath("$.data[0].destinationLatitude").value(21.012345));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "TRACKING_WRITE"})
    void rejectsCoordinatesOutsideLatitudeAndLongitudeRanges() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(patch("/api/trips/{id}/coordinates", trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationLatitude":91.0,"destinationLongitude":181.0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @WithMockUser(authorities = "ROLE_STAFF")
    void deniesCoordinateUpdatesWithoutTrackingWritePermission() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(patch("/api/trips/{id}/coordinates", trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originLatitude":10.0,"originLongitude":106.0}
                                """))
                .andExpect(status().isForbidden());
    }

    private Trip createTrip() {
        Partner partner = new Partner();
        partner.setCode("TRIP-COORD-" + UUID.randomUUID());
        partner.setName("Trip coordinate integration partner");
        partner.setCountryCode("VN");
        partner.setPartnerType("A");
        partner = partnerRepository.save(partner);

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("COORD-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setPartnerAId(partner.getId());
        vehicle.setDriverName("Integration driver");
        vehicle = vehicleRepository.save(vehicle);

        Trip trip = new Trip();
        trip.setVehicleId(vehicle.getId());
        trip.setTripCode("TRIP-COORD-" + UUID.randomUUID());
        trip.setTripName("Trip coordinate integration");
        trip.setOrigin("Origin");
        trip.setOriginLatitude(new BigDecimal("10.0000000"));
        trip.setOriginLongitude(new BigDecimal("106.0000000"));
        trip.setDestination("Destination");
        trip.setDestinationLatitude(new BigDecimal("20.000000"));
        trip.setDestinationLongitude(new BigDecimal("107.000000"));
        return tripRepository.save(trip);
    }
}
