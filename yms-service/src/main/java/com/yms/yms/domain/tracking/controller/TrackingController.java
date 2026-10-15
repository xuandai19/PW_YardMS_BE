package com.yms.yms.domain.tracking.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.tracking.entity.RouteAlert;
import com.yms.yms.domain.tracking.entity.RouteLog;
import com.yms.yms.domain.tracking.entity.TripGeofence;
import com.yms.yms.domain.tracking.service.TrackingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class TrackingController {
    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/gps")
    public ApiResponse<RouteLog> ingest(@Valid @RequestBody GpsRequest request) {
        return ApiResponse.ok(trackingService.ingest(new TrackingService.GpsPoint(request.tripId(),
                request.recordedAt(), request.latitude(), request.longitude(), request.speedKmh(),
                request.heading(), request.accuracyM(), request.source())));
    }

    @GetMapping("/trips/{tripId}/history")
    public ApiResponse<List<RouteLog>> history(@PathVariable UUID tripId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(trackingService.history(tripId, from, to));
    }

    @GetMapping("/trips/{tripId}/latest")
    public ApiResponse<RouteLog> latest(@PathVariable UUID tripId) {
        return ApiResponse.ok(trackingService.latest(tripId));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<RouteAlert>> alerts() {
        return ApiResponse.ok(trackingService.alerts());
    }

    @GetMapping("/trips/{tripId}/geofences")
    public ApiResponse<List<TripGeofence>> geofences(@PathVariable UUID tripId) {
        return ApiResponse.ok(trackingService.geofences(tripId));
    }

    @PostMapping("/trips/{tripId}/geofences")
    public ApiResponse<TripGeofence> addGeofence(@PathVariable UUID tripId,
            @Valid @RequestBody GeofenceRequest request) {
        return ApiResponse.ok(trackingService.addGeofence(tripId, new TrackingService.GeofenceRequest(
                request.name(), request.latitude(), request.longitude(), request.radiusM())));
    }

    public record GpsRequest(@NotNull UUID tripId, Instant recordedAt,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            BigDecimal speedKmh, BigDecimal heading, BigDecimal accuracyM, String source) {
    }

    public record GeofenceRequest(@NotBlank String name,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @NotNull @Positive BigDecimal radiusM) {
    }
}
