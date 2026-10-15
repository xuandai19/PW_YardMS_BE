package com.yms.yms.domain.tracking.repository;

import com.yms.yms.domain.tracking.entity.RouteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteLogRepository extends JpaRepository<RouteLog, UUID> {
    List<RouteLog> findByTripIdAndRecordedAtAfterOrderByRecordedAtAsc(UUID tripId, Instant after);
    Optional<RouteLog> findFirstByTripIdOrderByRecordedAtDesc(UUID tripId);

    @Modifying
    @Query("delete from RouteLog r where r.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
