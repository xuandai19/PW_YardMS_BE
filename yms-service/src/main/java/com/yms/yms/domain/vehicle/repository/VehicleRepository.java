package com.yms.yms.domain.vehicle.repository;

import com.yms.yms.domain.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findFirstByPlateNumberIgnoreCase(String plateNumber);
}
