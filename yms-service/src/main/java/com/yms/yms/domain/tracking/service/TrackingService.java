package com.yms.yms.domain.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.tracking.entity.RouteAlert;
import com.yms.yms.domain.tracking.entity.RouteLog;
import com.yms.yms.domain.tracking.entity.TripGeofence;
import com.yms.yms.domain.tracking.repository.RouteAlertRepository;
import com.yms.yms.domain.tracking.repository.RouteLogRepository;
import com.yms.yms.domain.tracking.repository.TripGeofenceRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TrackingService {
    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final RouteLogRepository routeLogs;
    private final TripGeofenceRepository geofences;
    private final RouteAlertRepository alerts;
    private final TripRepository trips;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final int retentionDays;
    private final boolean postgisEnabled;

    public TrackingService(RouteLogRepository routeLogs, TripGeofenceRepository geofences,
                           RouteAlertRepository alerts, TripRepository trips,
                           SimpMessagingTemplate messaging, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate, Clock clock,
                           @Value("${yms.tracking.retention-days:90}") int retentionDays,
                           @Value("${yms.tracking.postgis-enabled:false}") boolean postgisEnabled) {
        this.routeLogs = routeLogs;
        this.geofences = geofences;
        this.alerts = alerts;
        this.trips = trips;
        this.messaging = messaging;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.retentionDays = retentionDays;
        this.postgisEnabled = postgisEnabled;
    }

    @Transactional
    public RouteLog ingest(GpsPoint point) {
        if (point.tripId() == null || point.latitude() == null || point.longitude() == null
                || point.latitude().compareTo(BigDecimal.valueOf(-90)) < 0
                || point.latitude().compareTo(BigDecimal.valueOf(90)) > 0
                || point.longitude().compareTo(BigDecimal.valueOf(-180)) < 0
                || point.longitude().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException("Invalid GPS payload", HttpStatus.BAD_REQUEST);
        }
        if (!trips.existsById(point.tripId())) {
            throw new BusinessException("Trip not found", HttpStatus.NOT_FOUND);
        }

        Instant recordedAt = point.recordedAt() == null ? clock.instant() : point.recordedAt();
        if (Duration.between(recordedAt, clock.instant()).abs().compareTo(Duration.ofMinutes(5)) > 0) {
            throw new BusinessException("GPS timestamp must be within five minutes of server time",
                    HttpStatus.BAD_REQUEST);
        }
        List<TripGeofence> tripFences = geofences.findByTripIdAndActiveTrue(point.tripId());
        boolean offRoute = !tripFences.isEmpty() && (postgisEnabled
                ? !isWithinPostgisFence(point)
                : tripFences.stream().noneMatch(fence ->
                        distanceMeters(point.latitude().doubleValue(), point.longitude().doubleValue(),
                                fence.getLatitude().doubleValue(), fence.getLongitude().doubleValue())
                                <= fence.getRadiusM().doubleValue()));

        RouteLog routeLog = new RouteLog();
        routeLog.setId(UUID.randomUUID());
        routeLog.setTripId(point.tripId());
        routeLog.setRecordedAt(recordedAt);
        routeLog.setLatitude(point.latitude());
        routeLog.setLongitude(point.longitude());
        routeLog.setSpeedKmh(point.speedKmh());
        routeLog.setHeading(point.heading());
        routeLog.setAccuracyM(point.accuracyM());
        routeLog.setSource(point.source() == null || point.source().isBlank() ? "GPS" : point.source());
        routeLog.setOffRoute(offRoute);
        RouteLog saved = routeLogs.save(routeLog);

        if (offRoute) {
            RouteAlert alert = new RouteAlert();
            alert.setId(UUID.randomUUID());
            alert.setTripId(point.tripId());
            alert.setRouteLogId(saved.getId());
            alert.setMessage("Xe đã rời khỏi vùng geofence đã cấu hình cho chuyến đi");
            alert.setCreatedAt(clock.instant());
            alert.setAcknowledged(false);
            alerts.save(alert);
            messaging.convertAndSend("/topic/tracking/alerts", alert);
        }
        messaging.convertAndSend("/topic/trips/" + point.tripId() + "/location", saved);
        messaging.convertAndSend("/topic/tracking/latest", saved);
        return saved;
    }

    public List<RouteLog> history(UUID tripId, Instant from, Instant to) {
        if (!trips.existsById(tripId)) {
            throw new BusinessException("Trip not found", HttpStatus.NOT_FOUND);
        }
        Instant earliest = clock.instant().minus(Duration.ofDays(retentionDays));
        Instant start = from == null || from.isBefore(earliest) ? earliest : from;
        if (to != null && to.isBefore(start)) {
            throw new BusinessException("End time must be after start time", HttpStatus.BAD_REQUEST);
        }
        return routeLogs.findByTripIdAndRecordedAtAfterOrderByRecordedAtAsc(tripId, start).stream()
                .filter(log -> to == null || !log.getRecordedAt().isAfter(to))
                .toList();
    }

    public RouteLog latest(UUID tripId) {
        return routeLogs.findFirstByTripIdOrderByRecordedAtDesc(tripId)
                .orElseThrow(() -> new BusinessException("No GPS points found for trip", HttpStatus.NOT_FOUND));
    }

    public List<RouteAlert> alerts() {
        return alerts.findTop100ByOrderByCreatedAtDesc();
    }

    public List<TripGeofence> geofences(UUID tripId) {
        if (!trips.existsById(tripId)) {
            throw new BusinessException("Trip not found", HttpStatus.NOT_FOUND);
        }
        return geofences.findByTripIdAndActiveTrue(tripId);
    }

    @Transactional
    public TripGeofence addGeofence(UUID tripId, GeofenceRequest request) {
        if (!trips.existsById(tripId)) {
            throw new BusinessException("Trip not found", HttpStatus.NOT_FOUND);
        }
        TripGeofence fence = new TripGeofence();
        fence.setId(UUID.randomUUID());
        fence.setTripId(tripId);
        fence.setName(request.name());
        fence.setLatitude(request.latitude());
        fence.setLongitude(request.longitude());
        fence.setRadiusM(request.radiusM());
        fence.setActive(true);
        TripGeofence saved = geofences.saveAndFlush(fence);
        if (postgisEnabled) {
            jdbcTemplate.update("UPDATE trip_geofences SET center = "
                    + "ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography WHERE id = ?",
                    request.longitude(), request.latitude(), saved.getId());
        }
        return saved;
    }

    private boolean isWithinPostgisFence(GpsPoint point) {
        Integer matchCount = jdbcTemplate.queryForObject("SELECT count(*) FROM trip_geofences "
                + "WHERE trip_id = ? AND active = TRUE AND ST_DWithin(center, "
                + "ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, radius_m)",
                Integer.class, point.tripId(), point.longitude(), point.latitude());
        return matchCount != null && matchCount > 0;
    }

    @Scheduled(cron = "0 15 2 * * *")
    @Transactional
    public void purgeExpiredRouteLogs() {
        int removed = routeLogs.deleteOlderThan(clock.instant().minus(Duration.ofDays(retentionDays)));
        if (removed > 0) {
            log.info("Removed {} GPS records older than {} days", removed, retentionDays);
        }
    }

    public GpsPoint parse(String payload) {
        try {
            return objectMapper.readValue(payload, GpsPoint.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid GPS event JSON", exception);
        }
    }

    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record GpsPoint(UUID tripId, Instant recordedAt, BigDecimal latitude, BigDecimal longitude,
                           BigDecimal speedKmh, BigDecimal heading, BigDecimal accuracyM, String source) {
    }

    public record GeofenceRequest(String name, BigDecimal latitude, BigDecimal longitude, BigDecimal radiusM) {
    }
}
