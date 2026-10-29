ALTER TABLE trips
    ADD COLUMN destination_yard_id UUID REFERENCES yards(id),
    ADD COLUMN destination_latitude NUMERIC(9, 6),
    ADD COLUMN destination_longitude NUMERIC(9, 6);

ALTER TABLE trips
    ADD CONSTRAINT chk_trip_destination_coordinates
    CHECK ((destination_latitude IS NULL AND destination_longitude IS NULL)
        OR (destination_latitude BETWEEN -90 AND 90 AND destination_longitude BETWEEN -180 AND 180));
