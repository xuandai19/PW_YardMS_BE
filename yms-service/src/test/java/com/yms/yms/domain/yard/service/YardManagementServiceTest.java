package com.yms.yms.domain.yard.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.vehicle.entity.Vehicle;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
import com.yms.yms.domain.yard.entity.Yard;
import com.yms.yms.domain.yard.repository.YardAlertRepository;
import com.yms.yms.domain.yard.repository.YardRepository;
import com.yms.yms.domain.yard.repository.YardVehicleAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YardManagementServiceTest {

    @Mock YardRepository yardRepository;
    @Mock YardVehicleAssignmentRepository assignmentRepository;
    @Mock YardAlertRepository alertRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @Test
    void preventsVehicleFromHavingTwoOpenYardAssignments() {
        var service = service();
        UUID yardId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Yard yard = new Yard();
        yard.setId(yardId);
        yard.setCapacity(10);
        yard.setStatus("ACTIVE");
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        when(yardRepository.findByIdForUpdate(yardId)).thenReturn(Optional.of(yard));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(assignmentRepository.findByVehicleIdAndCheckOutAtIsNull(vehicleId))
                .thenReturn(Optional.of(new com.yms.yms.domain.yard.entity.YardVehicleAssignment()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.assignVehicle(yardId, vehicleId, Instant.now()));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void warnsAtNinetyPercentAndNotBelowThreshold() {
        assertFalse(YardManagementService.isAtAlertThreshold(89, 100));
        assertTrue(YardManagementService.isAtAlertThreshold(90, 100));
        assertTrue(YardManagementService.isAtAlertThreshold(9, 10));
    }

    private YardManagementService service() {
        return new YardManagementService(yardRepository, assignmentRepository, alertRepository, vehicleRepository,
                messagingTemplate, Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC));
    }
}
