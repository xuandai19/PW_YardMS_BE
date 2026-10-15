package com.yms.yms.domain.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yms.yms.domain.tracking.entity.RouteLog;
import com.yms.yms.domain.tracking.entity.TripGeofence;
import com.yms.yms.domain.tracking.repository.RouteAlertRepository;
import com.yms.yms.domain.tracking.repository.RouteLogRepository;
import com.yms.yms.domain.tracking.repository.TripGeofenceRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackingServiceTest {
    private final RouteLogRepository routeLogs = mock(RouteLogRepository.class);
    private final TripGeofenceRepository geofences = mock(TripGeofenceRepository.class);
    private final RouteAlertRepository alerts = mock(RouteAlertRepository.class);
    private final TripRepository trips = mock(TripRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private TrackingService service;

    @BeforeEach
    void setUp() {
        service = new TrackingService(routeLogs, geofences, alerts, trips, messaging, new ObjectMapper(),
                mock(JdbcTemplate.class), clock, 90, false);
    }

    @Test
    void persistsGpsAndRaisesAlertOutsideConfiguredFence() {
        UUID tripId = UUID.randomUUID();
        TripGeofence fence = new TripGeofence();
        fence.setLatitude(BigDecimal.valueOf(10));
        fence.setLongitude(BigDecimal.valueOf(106));
        fence.setRadiusM(BigDecimal.valueOf(100));
        when(trips.existsById(tripId)).thenReturn(true);
        when(geofences.findByTripIdAndActiveTrue(tripId)).thenReturn(List.of(fence));
        when(routeLogs.save(any(RouteLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteLog saved = service.ingest(new TrackingService.GpsPoint(tripId, clock.instant(),
                BigDecimal.valueOf(11), BigDecimal.valueOf(107), null, null, null, "GPS"));

        assertTrue(saved.isOffRoute());
        verify(alerts).save(any());
        verify(messaging).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/tracking/alerts"),
                org.mockito.ArgumentMatchers.any(Object.class));
        verify(messaging).convertAndSend("/topic/trips/" + tripId + "/location", saved);
    }

    @Test
    void keepsGpsInsideFenceOnRoute() {
        UUID tripId = UUID.randomUUID();
        TripGeofence fence = new TripGeofence();
        fence.setLatitude(BigDecimal.valueOf(10));
        fence.setLongitude(BigDecimal.valueOf(106));
        fence.setRadiusM(BigDecimal.valueOf(500));
        when(trips.existsById(tripId)).thenReturn(true);
        when(geofences.findByTripIdAndActiveTrue(tripId)).thenReturn(List.of(fence));
        when(routeLogs.save(any(RouteLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteLog saved = service.ingest(new TrackingService.GpsPoint(tripId, clock.instant(),
                BigDecimal.valueOf(10.001), BigDecimal.valueOf(106.001), null, null, null, null));

        assertFalse(saved.isOffRoute());
    }

    @Test
    void rejectsGpsOlderThanFiveMinutes() {
        UUID tripId = UUID.randomUUID();
        when(trips.existsById(tripId)).thenReturn(true);
        Instant stale = clock.instant().minusSeconds(301);

        assertThrows(RuntimeException.class, () -> service.ingest(new TrackingService.GpsPoint(tripId, stale,
                BigDecimal.TEN, BigDecimal.TEN, null, null, null, null)));
    }
}
