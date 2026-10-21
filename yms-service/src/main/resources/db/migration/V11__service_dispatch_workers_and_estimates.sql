ALTER TABLE services
    ADD COLUMN IF NOT EXISTS expected_duration_minutes INTEGER NOT NULL DEFAULT 60
    CHECK (expected_duration_minutes > 0);

ALTER TABLE service_orders
    ADD COLUMN IF NOT EXISTS expected_end_at TIMESTAMPTZ;

UPDATE services
SET expected_duration_minutes = CASE service_type
    WHEN 'YARD' THEN 60
    WHEN 'HANDLING' THEN 90
    WHEN 'MAINTENANCE' THEN 120
    WHEN 'FUEL' THEN 30
    ELSE 60
END
WHERE expected_duration_minutes = 60;

UPDATE service_orders so
SET expected_end_at = COALESCE(so.start_at, so.created_at) + make_interval(mins => s.expected_duration_minutes)
FROM services s
WHERE so.service_id = s.id AND so.expected_end_at IS NULL;

CREATE TABLE IF NOT EXISTS yard_service_workers (
    id UUID PRIMARY KEY,
    yard_id UUID NOT NULL REFERENCES yards(id),
    service_type VARCHAR(30) NOT NULL CHECK (service_type IN ('YARD', 'HANDLING', 'MAINTENANCE', 'FUEL', 'OTHER')),
    worker_name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (yard_id, service_type, worker_name)
);

-- Sample roster so availability can be demonstrated immediately at existing active yards.
INSERT INTO yard_service_workers (id, yard_id, service_type, worker_name, active)
SELECT (substr(md5(y.id::text || ':' || s.service_type || ':1'), 1, 8) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':1'), 9, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':1'), 13, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':1'), 17, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':1'), 21, 12))::uuid,
       y.id, s.service_type, 'Nhân viên ' || s.service_type || ' 01', TRUE
FROM yards y
CROSS JOIN (SELECT DISTINCT service_type FROM services WHERE active = TRUE) s
WHERE y.status = 'ACTIVE'
ON CONFLICT (yard_id, service_type, worker_name) DO NOTHING;

INSERT INTO yard_service_workers (id, yard_id, service_type, worker_name, active)
SELECT (substr(md5(y.id::text || ':' || s.service_type || ':2'), 1, 8) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':2'), 9, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':2'), 13, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':2'), 17, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':2'), 21, 12))::uuid,
       y.id, s.service_type, 'Nhân viên ' || s.service_type || ' 02', TRUE
FROM yards y
CROSS JOIN (SELECT DISTINCT service_type FROM services WHERE active = TRUE) s
WHERE y.status = 'ACTIVE'
ON CONFLICT (yard_id, service_type, worker_name) DO NOTHING;
