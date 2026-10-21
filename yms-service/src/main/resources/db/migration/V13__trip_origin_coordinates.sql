ALTER TABLE trips ADD COLUMN IF NOT EXISTS origin_latitude NUMERIC(10, 7);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS origin_longitude NUMERIC(10, 7);

ALTER TABLE trips DROP CONSTRAINT IF EXISTS chk_trip_origin_coordinates;
ALTER TABLE trips ADD CONSTRAINT chk_trip_origin_coordinates CHECK (
    (origin_latitude IS NULL AND origin_longitude IS NULL)
    OR (origin_latitude IS NOT NULL AND origin_longitude IS NOT NULL
        AND origin_latitude BETWEEN -90 AND 90 AND origin_longitude BETWEEN -180 AND 180)
);
