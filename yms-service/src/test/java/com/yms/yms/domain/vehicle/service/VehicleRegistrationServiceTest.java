package com.yms.yms.domain.vehicle.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import com.yms.yms.domain.service.service.ServiceDispatchService;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.entity.Vehicle;
import com.yms.yms.domain.vehicle.repository.PreNotificationRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleRegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T00:00:00Z");

    @Mock VehicleRepository vehicleRepository;
    @Mock PreNotificationRepository notificationRepository;
    @Mock TripRepository tripRepository;
    @Mock PartnerRepository partnerRepository;
    @Mock ServiceDispatchService dispatchService;

    private VehicleRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new VehicleRegistrationService(vehicleRepository, notificationRepository, tripRepository,
                partnerRepository, dispatchService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsArrivalLessThanFourHoursInAdvance() {
        var request = request(UUID.randomUUID(), NOW.plusSeconds(4 * 60 * 60 - 1));

        BusinessException error = assertThrows(BusinessException.class, () -> service.register(request));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(partnerRepository, vehicleRepository, tripRepository);
    }

    @Test
    void acceptsExactlyFourHoursAndNormalizesPlate() {
        UUID partnerId = UUID.randomUUID();
        var partner = new com.yms.yms.domain.partner.entity.Partner();
        partner.setId(partnerId);
        partner.setPartnerType("A");
        partner.setActive(true);
        when(notificationRepository.existsByPlateNumberAndArrivalDate("51C12345",
                NOW.plusSeconds(4 * 60 * 60).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate()))
                .thenReturn(false);
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(vehicleRepository.findFirstByPlateNumberIgnoreCase("51C12345")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(UUID.randomUUID());
            return vehicle;
        });
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setId(UUID.randomUUID());
            return trip;
        });
        when(notificationRepository.saveAndFlush(any(PreNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreNotification result = service.register(request(partnerId, NOW.plusSeconds(4 * 60 * 60)));

        assertEquals("51C12345", result.getPlateNumber());
        verify(notificationRepository).saveAndFlush(any(PreNotification.class));
        verifyNoInteractions(dispatchService);
    }

    private VehicleRegistrationService.RegistrationRequest request(UUID partnerId, Instant arrival) {
        return new VehicleRegistrationService.RegistrationRequest(
                partnerId, " 51c 12345 ", arrival, "Driver", null, null, null, List.of());
    }
}
