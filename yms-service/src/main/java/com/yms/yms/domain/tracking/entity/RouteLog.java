package com.yms.yms.domain.tracking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_logs")
@Getter
@Setter
@NoArgsConstructor
public class RouteLog {
    @Id
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "speed_kmh", precision = 8, scale = 2)
    private BigDecimal speedKmh;

    @Column(precision = 6, scale = 2)
    private BigDecimal heading;

    @Column(name = "accuracy_m", precision = 8, scale = 2)
    private BigDecimal accuracyM;

    @Column(nullable = false, length = 30)
    private String source = "GPS";

    @Column(name = "off_route", nullable = false)
    private boolean offRoute;
}
