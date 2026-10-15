ALTER TABLE goods ADD COLUMN qr_value VARCHAR(500);
UPDATE goods SET qr_value = 'YMS-GOODS:' || id::text WHERE qr_value IS NULL;
CREATE UNIQUE INDEX uq_goods_qr_value ON goods(qr_value) WHERE qr_value IS NOT NULL;
CREATE INDEX idx_route_logs_recorded_at ON route_logs(recorded_at);

CREATE TABLE trip_geofences (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    radius_m NUMERIC(10, 2) NOT NULL CHECK (radius_m > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_trip_geofence_trip FOREIGN KEY (trip_id) REFERENCES trips(id)
);
CREATE INDEX idx_trip_geofences_trip ON trip_geofences(trip_id, active);

CREATE TABLE route_alerts (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    route_log_id UUID NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_route_alert_trip FOREIGN KEY (trip_id) REFERENCES trips(id)
);
CREATE INDEX idx_route_alert_trip_created ON route_alerts(trip_id, created_at DESC);

CREATE TABLE goods_discrepancies (
    id UUID PRIMARY KEY,
    goods_id UUID NOT NULL,
    expected_quantity NUMERIC(19, 4) NOT NULL,
    scanned_quantity NUMERIC(19, 4) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_goods_discrepancy_goods FOREIGN KEY (goods_id) REFERENCES goods(id)
);
CREATE INDEX idx_goods_discrepancy_goods ON goods_discrepancies(goods_id, created_at DESC);

CREATE TABLE driver_confirmations (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    confirmation_type VARCHAR(20) NOT NULL CHECK (confirmation_type IN ('DEPARTURE', 'ARRIVAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'OVERDUE', 'LATE')),
    requested_at TIMESTAMPTZ NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    signature_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_driver_confirmation_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT uq_driver_confirmation_event UNIQUE (trip_id, confirmation_type)
);
CREATE INDEX idx_driver_confirmation_due ON driver_confirmations(status, due_at);

CREATE TABLE driver_confirmation_alerts (
    id UUID PRIMARY KEY,
    confirmation_id UUID NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_driver_confirmation_alert FOREIGN KEY (confirmation_id) REFERENCES driver_confirmations(id)
);
CREATE INDEX idx_driver_confirmation_alerts_created ON driver_confirmation_alerts(created_at DESC);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb') THEN
        EXECUTE 'CREATE EXTENSION IF NOT EXISTS timescaledb';
        IF NOT EXISTS (SELECT 1 FROM timescaledb_information.hypertables WHERE hypertable_name = 'route_logs') THEN
            ALTER TABLE route_logs DROP CONSTRAINT route_logs_pkey;
            ALTER TABLE route_logs ADD PRIMARY KEY (id, recorded_at);
            PERFORM create_hypertable('route_logs', 'recorded_at', if_not_exists => TRUE, migrate_data => TRUE);
        END IF;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'postgis') THEN
        EXECUTE 'CREATE EXTENSION IF NOT EXISTS postgis';
        EXECUTE 'ALTER TABLE trip_geofences ADD COLUMN IF NOT EXISTS center geography(Point, 4326)';
        EXECUTE 'UPDATE trip_geofences SET center = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography WHERE center IS NULL';
    END IF;
END $$;
