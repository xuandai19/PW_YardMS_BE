package com.yms.yms.domain.yard.repository;

import com.yms.yms.domain.yard.entity.YardVehicleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface YardVehicleAssignmentRepository extends JpaRepository<YardVehicleAssignment, UUID> {
    Optional<YardVehicleAssignment> findByVehicleIdAndCheckOutAtIsNull(UUID vehicleId);
    List<YardVehicleAssignment> findByYardIdAndCheckOutAtIsNull(UUID yardId);

    @Query("select count(a) from YardVehicleAssignment a where a.yardId = :yardId and a.checkOutAt is null")
    long countCurrentByYardId(@Param("yardId") UUID yardId);

    @Query("""
            select a from YardVehicleAssignment a
            where a.checkInAt < :to
              and (a.checkOutAt is null or a.checkOutAt > :from)
            """)
    List<YardVehicleAssignment> findOverlappingPeriod(@Param("from") Instant from, @Param("to") Instant to);
}
