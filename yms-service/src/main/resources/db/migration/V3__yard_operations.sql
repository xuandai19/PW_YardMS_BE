CREATE TABLE pre_notifications (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    partner_a_id UUID NOT NULL,
    trip_id UUID NOT NULL UNIQUE,
    plate_number VARCHAR(30) NOT NULL,
    arrival_date DATE NOT NULL,
    planned_arrival TIMESTAMPTZ NOT NULL,
    driver_name VARCHAR(150) NOT NULL,
    driver_phone VARCHAR(30),
    carrier_company VARCHAR(255),
    vehicle_type VARCHAR(80),
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED'
        CHECK (status IN ('REGISTERED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    qr_token VARCHAR(500),
    qr_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pre_notification_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_pre_notification_partner FOREIGN KEY (partner_a_id) REFERENCES partners(id),
    CONSTRAINT fk_pre_notification_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT uq_pre_notification_plate_day UNIQUE (plate_number, arrival_date)
);

CREATE INDEX idx_pre_notifications_arrival_status
    ON pre_notifications (arrival_date, status, planned_arrival);

CREATE TABLE yard_logs (
    id UUID PRIMARY KEY,
    pre_notification_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    yard_id UUID NOT NULL,
    check_in_at TIMESTAMPTZ NOT NULL,
    check_out_at TIMESTAMPTZ,
    duration_seconds BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_yard_log_notification FOREIGN KEY (pre_notification_id) REFERENCES pre_notifications(id),
    CONSTRAINT fk_yard_log_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_yard_log_yard FOREIGN KEY (yard_id) REFERENCES yards(id),
    CONSTRAINT chk_yard_log_times CHECK (check_out_at IS NULL OR check_out_at >= check_in_at),
    CONSTRAINT chk_yard_log_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE INDEX idx_yard_logs_vehicle_time ON yard_logs (vehicle_id, check_in_at DESC);
CREATE UNIQUE INDEX uq_yard_log_open_notification ON yard_logs (pre_notification_id) WHERE check_out_at IS NULL;
CREATE UNIQUE INDEX uq_yard_assignment_open_vehicle ON yard_vehicle_assignments (vehicle_id) WHERE check_out_at IS NULL;
CREATE UNIQUE INDEX uq_service_order_trip_service ON service_orders (trip_id, service_id);

CREATE TABLE yard_alerts (
    id UUID PRIMARY KEY,
    yard_id UUID NOT NULL,
    occupancy INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    alert_type VARCHAR(30) NOT NULL DEFAULT 'CAPACITY_90',
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_yard_alert_yard FOREIGN KEY (yard_id) REFERENCES yards(id),
    CONSTRAINT chk_yard_alert_occupancy CHECK (occupancy >= 0 AND capacity > 0)
);

CREATE INDEX idx_yard_alerts_active ON yard_alerts (yard_id, created_at DESC) WHERE resolved_at IS NULL;
