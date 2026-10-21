ALTER TABLE trips ADD COLUMN IF NOT EXISTS trip_code VARCHAR(80);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS trip_name VARCHAR(255);

UPDATE trips t
SET trip_code = COALESCE(t.trip_code,
        'TRIP-' || TO_CHAR(COALESCE(t.planned_at, t.created_at), 'YYYYMMDD') || '-' ||
        REGEXP_REPLACE(UPPER(v.plate_number), '[^A-Z0-9]', '', 'g') || '-' || SUBSTRING(t.id::text, 1, 6)),
    trip_name = COALESCE(t.trip_name,
        'Chuyến ' || UPPER(v.plate_number) || ' - ' ||
        TO_CHAR(COALESCE(t.planned_at, t.created_at), 'DD/MM/YYYY'))
FROM vehicles v
WHERE t.vehicle_id = v.id;

ALTER TABLE trips ALTER COLUMN trip_code SET NOT NULL;
ALTER TABLE trips ALTER COLUMN trip_name SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_trips_trip_code ON trips (trip_code);
