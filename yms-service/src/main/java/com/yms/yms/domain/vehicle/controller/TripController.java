package com.yms.yms.domain.vehicle.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class TripController {
    private final TripRepository tripRepository;

    public TripController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @GetMapping
    public ApiResponse<List<Trip>> list() {
        return ApiResponse.ok(tripRepository.findAll());
    }
}
