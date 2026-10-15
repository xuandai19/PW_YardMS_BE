package com.yms.yms.domain.tracking.repository;

import com.yms.yms.domain.tracking.entity.TripGeofence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripGeofenceRepository extends JpaRepository<TripGeofence, UUID> {
    List<TripGeofence> findByTripIdAndActiveTrue(UUID tripId);
}
