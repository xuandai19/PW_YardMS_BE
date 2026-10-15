package com.yms.yms.domain.vehicle.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.repository.PreNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T00:00:00Z");

    @Mock PreNotificationRepository notificationRepository;
    private PreNotification notification;
    private QrCodeService service;

    @BeforeEach
    void setUp() {
        notification = new PreNotification();
        notification.setId(UUID.randomUUID());
        notification.setStatus("REGISTERED");
        service = new QrCodeService(notificationRepository, Clock.fixed(NOW, ZoneOffset.UTC),
                "unit-test-hmac-secret-with-at-least-32-bytes");
    }

    @Test
    void issuesSignedQrValidForTwentyFourHours() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QrCodeService.QrResult result = service.issue(notification.getId());

        assertEquals(NOW.plusSeconds(86_400), result.expiresAt());
        assertFalse(result.imageBase64().isBlank());
        assertSame(notification, service.verify(result.token()));
    }

    @Test
    void rejectsExpiredQr() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        QrCodeService.QrResult result = service.issue(notification.getId());

        Clock expiredClock = Clock.fixed(NOW.plusSeconds(86_400), ZoneOffset.UTC);
        QrCodeService expiredService = new QrCodeService(notificationRepository, expiredClock,
                "unit-test-hmac-secret-with-at-least-32-bytes");

        BusinessException error = assertThrows(BusinessException.class, () -> expiredService.verify(result.token()));

        assertEquals(HttpStatus.GONE, error.getStatus());
    }

    @Test
    void rejectsTamperedSignature() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String token = service.issue(notification.getId()).token();
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalid";

        BusinessException error = assertThrows(BusinessException.class, () -> service.verify(tampered));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }
}
