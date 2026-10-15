package com.yms.yms.domain.confirmation.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.confirmation.entity.DriverConfirmation;
import com.yms.yms.domain.confirmation.entity.DriverConfirmationAlert;
import com.yms.yms.domain.confirmation.repository.DriverConfirmationAlertRepository;
import com.yms.yms.domain.confirmation.repository.DriverConfirmationRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.infrastructure.storage.DriverSignatureStorageService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DriverConfirmationService {
    private final DriverConfirmationRepository confirmations;
    private final DriverConfirmationAlertRepository alerts;
    private final TripRepository trips;
    private final DriverSignatureStorageService storage;
    private final SimpMessagingTemplate messaging;
    private final Clock clock;

    public DriverConfirmationService(DriverConfirmationRepository confirmations,
            DriverConfirmationAlertRepository alerts, TripRepository trips,
            DriverSignatureStorageService storage, SimpMessagingTemplate messaging, Clock clock) {
        this.confirmations = confirmations;
        this.alerts = alerts;
        this.trips = trips;
        this.storage = storage;
        this.messaging = messaging;
        this.clock = clock;
    }

    @Transactional
    public DriverConfirmation request(UUID tripId, String type) {
        if (!trips.existsById(tripId)) {
            throw new BusinessException("Trip not found", HttpStatus.NOT_FOUND);
        }
        if (!"DEPARTURE".equals(type) && !"ARRIVAL".equals(type)) {
            throw new BusinessException("Confirmation type must be DEPARTURE or ARRIVAL", HttpStatus.BAD_REQUEST);
        }
        if (confirmations.findByTripIdOrderByRequestedAt(tripId).stream()
                .anyMatch(existing -> existing.getConfirmationType().equals(type))) {
            throw new BusinessException("Confirmation has already been requested for this trip event",
                    HttpStatus.CONFLICT);
        }
        Instant now = clock.instant();
        DriverConfirmation confirmation = new DriverConfirmation();
        confirmation.setId(UUID.randomUUID());
        confirmation.setTripId(tripId);
        confirmation.setConfirmationType(type);
        confirmation.setStatus("PENDING");
        confirmation.setRequestedAt(now);
        confirmation.setDueAt(now.plus(Duration.ofMinutes(30)));
        confirmation.setCreatedAt(now);
        DriverConfirmation saved = confirmations.save(confirmation);
        messaging.convertAndSend("/topic/driver-confirmations", saved);
        return saved;
    }

    public List<DriverConfirmation> forTrip(UUID tripId) {
        return confirmations.findByTripIdOrderByRequestedAt(tripId);
    }

    @Transactional
    public DriverConfirmation confirm(UUID confirmationId) {
        DriverConfirmation confirmation = confirmations.findById(confirmationId)
                .orElseThrow(() -> new BusinessException("Confirmation not found", HttpStatus.NOT_FOUND));
        if ("CONFIRMED".equals(confirmation.getStatus()) || "LATE".equals(confirmation.getStatus())) {
            throw new BusinessException("Confirmation has already been submitted", HttpStatus.CONFLICT);
        }
        Instant now = clock.instant();
        confirmation.setConfirmedAt(now);
        confirmation.setStatus(now.isAfter(confirmation.getDueAt()) ? "LATE" : "CONFIRMED");
        DriverConfirmation saved = confirmations.save(confirmation);
        messaging.convertAndSend("/topic/driver-confirmations", saved);
        return saved;
    }

    @Transactional
    public DriverConfirmation storeSignature(UUID confirmationId, MultipartFile file) {
        DriverConfirmation confirmation = confirmations.findById(confirmationId)
                .orElseThrow(() -> new BusinessException("Confirmation not found", HttpStatus.NOT_FOUND));
        confirmation.setSignatureUrl(storage.store(confirmationId, file));
        return confirmations.save(confirmation);
    }

    public List<DriverConfirmationAlert> alerts() {
        return alerts.findTop100ByOrderByCreatedAtDesc();
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void markOverdue() {
        Instant now = clock.instant();
        List<DriverConfirmation> overdue = confirmations.findByStatusAndDueAtBefore("PENDING", now);
        for (DriverConfirmation confirmation : overdue) {
            confirmation.setStatus("OVERDUE");
            confirmations.save(confirmation);
            DriverConfirmationAlert alert = new DriverConfirmationAlert();
            alert.setId(UUID.randomUUID());
            alert.setConfirmationId(confirmation.getId());
            alert.setMessage(confirmation.getConfirmationType() + " confirmation overdue for trip "
                    + confirmation.getTripId());
            alert.setCreatedAt(now);
            alert.setAcknowledged(false);
            DriverConfirmationAlert savedAlert = alerts.save(alert);
            messaging.convertAndSend("/topic/driver-confirmation-alerts", savedAlert);
        }
    }
}
