package com.yms.yms.domain.vehicle.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.domain.vehicle.service.TripService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class TripController {
    private final TripRepository tripRepository;
    private final TripService tripService;

    public TripController(TripRepository tripRepository, TripService tripService) {
        this.tripRepository = tripRepository;
        this.tripService = tripService;
    }

    @GetMapping
    public ApiResponse<List<Trip>> list() {
        return ApiResponse.ok(tripRepository.findAll());
    }

    @PatchMapping("/{id}/coordinates")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TRACKING_WRITE')")
    public ApiResponse<Trip> updateCoordinates(@PathVariable UUID id,
            @Valid @RequestBody CoordinatesRequest request) {
        return ApiResponse.ok(tripService.updateCoordinates(id, request.originLatitude(),
                request.originLongitude(), request.destinationLatitude(), request.destinationLongitude()));
    }

    public record CoordinatesRequest(
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            BigDecimal originLatitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            BigDecimal originLongitude,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            BigDecimal destinationLatitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            BigDecimal destinationLongitude) {
        @AssertTrue(message = "Provide a complete latitude and longitude pair for each point")
        public boolean isCoordinatePairValid() {
            boolean originSpecified = originLatitude != null || originLongitude != null;
            boolean destinationSpecified = destinationLatitude != null || destinationLongitude != null;
            boolean originComplete = originLatitude != null && originLongitude != null;
            boolean destinationComplete = destinationLatitude != null && destinationLongitude != null;
            return (originSpecified == originComplete)
                    && (destinationSpecified == destinationComplete)
                    && (originSpecified || destinationSpecified);
        }
    }
}
