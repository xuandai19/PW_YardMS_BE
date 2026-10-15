package com.yms.yms.domain.confirmation.service;

import com.yms.yms.domain.confirmation.entity.DriverConfirmation;
import com.yms.yms.domain.confirmation.entity.DriverConfirmationAlert;
import com.yms.yms.domain.confirmation.repository.DriverConfirmationAlertRepository;
import com.yms.yms.domain.confirmation.repository.DriverConfirmationRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.infrastructure.storage.DriverSignatureStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverConfirmationServiceTest {
    private final DriverConfirmationRepository confirmations = mock(DriverConfirmationRepository.class);
    private final DriverConfirmationAlertRepository alerts = mock(DriverConfirmationAlertRepository.class);
    private final TripRepository trips = mock(TripRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private final DriverConfirmationService service = new DriverConfirmationService(confirmations, alerts, trips,
            mock(DriverSignatureStorageService.class), messaging, clock);

    @Test
    void requestsThirtyMinuteSlaAndAcceptsOnTimeConfirmation() {
        UUID tripId = UUID.randomUUID();
        when(trips.existsById(tripId)).thenReturn(true);
        when(confirmations.findByTripIdOrderByRequestedAt(tripId)).thenReturn(List.of());
        when(confirmations.save(any(DriverConfirmation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverConfirmation requested = service.request(tripId, "DEPARTURE");
        when(confirmations.findById(requested.getId())).thenReturn(java.util.Optional.of(requested));
        assertEquals(clock.instant().plusSeconds(1800), requested.getDueAt());
        DriverConfirmation confirmed = service.confirm(requested.getId());

        assertEquals("CONFIRMED", confirmed.getStatus());
        assertEquals(clock.instant(), confirmed.getConfirmedAt());
    }

    @Test
    void schedulerMarksExpiredConfirmationAndCreatesAlert() {
        DriverConfirmation confirmation = new DriverConfirmation();
        confirmation.setId(UUID.randomUUID());
        confirmation.setTripId(UUID.randomUUID());
        confirmation.setConfirmationType("ARRIVAL");
        confirmation.setStatus("PENDING");
        confirmation.setDueAt(clock.instant().minusSeconds(1));
        when(confirmations.findByStatusAndDueAtBefore("PENDING", clock.instant())).thenReturn(List.of(confirmation));
        when(confirmations.save(any(DriverConfirmation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alerts.save(any(DriverConfirmationAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markOverdue();

        assertEquals("OVERDUE", confirmation.getStatus());
        verify(alerts).save(any(DriverConfirmationAlert.class));
        verify(messaging).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/driver-confirmation-alerts"),
                org.mockito.ArgumentMatchers.any(Object.class));
    }
}
