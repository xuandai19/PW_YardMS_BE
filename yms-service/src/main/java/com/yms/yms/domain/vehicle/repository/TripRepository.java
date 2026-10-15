package com.yms.yms.domain.vehicle.repository;

import com.yms.yms.domain.vehicle.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
}
