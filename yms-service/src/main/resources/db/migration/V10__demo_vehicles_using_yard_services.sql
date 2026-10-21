-- Two sample vehicles currently checked into the demo yard, with active service orders.
-- Reserved DEMO identifiers keep these records separate from operational data.

INSERT INTO vehicles (id, plate_number, partner_a_id, carrier_company, vehicle_type, driver_name, status)
SELECT v.id, v.plate_number, p.id, 'Vận tải Mẫu', 'Truck', v.driver_name, 'IN_YARD'
FROM (VALUES
    ('a1000000-0000-0000-0000-000000000001'::uuid, 'DEMO-INYARD-01', 'Nguyễn Minh'),
    ('a1000000-0000-0000-0000-000000000002'::uuid, 'DEMO-INYARD-02', 'Trần Hải')
) AS v(id, plate_number, driver_name)
CROSS JOIN partners p
WHERE p.code = 'DEMO-PARTNER-A'
ON CONFLICT (id) DO NOTHING;

INSERT INTO trips (id, vehicle_id, origin, destination, planned_at, departed_at, status)
SELECT t.id, v.id, 'Bãi mẫu Hà Nội', 'Kho vận mẫu Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 hour',
       CURRENT_TIMESTAMP - INTERVAL '45 minutes', 'IN_PROGRESS'
FROM (VALUES
    ('a2000000-0000-0000-0000-000000000001'::uuid, 'DEMO-INYARD-01'),
    ('a2000000-0000-0000-0000-000000000002'::uuid, 'DEMO-INYARD-02')
) AS t(id, plate_number)
JOIN vehicles v ON v.plate_number = t.plate_number
ON CONFLICT (id) DO NOTHING;

INSERT INTO pre_notifications (id, vehicle_id, partner_a_id, trip_id, plate_number, arrival_date, planned_arrival,
                              driver_name, driver_phone, carrier_company, vehicle_type, status)
SELECT n.id, v.id, p.id, t.id, v.plate_number, CURRENT_DATE, CURRENT_TIMESTAMP - INTERVAL '30 minutes',
       n.driver_name, NULL, 'Vận tải Mẫu', 'Truck', 'CHECKED_IN'
FROM (VALUES
    ('a3000000-0000-0000-0000-000000000001'::uuid, 'a2000000-0000-0000-0000-000000000001'::uuid, 'DEMO-INYARD-01', 'Nguyễn Minh'),
    ('a3000000-0000-0000-0000-000000000002'::uuid, 'a2000000-0000-0000-0000-000000000002'::uuid, 'DEMO-INYARD-02', 'Trần Hải')
) AS n(id, trip_id, plate_number, driver_name)
JOIN trips t ON t.id = n.trip_id
JOIN vehicles v ON v.plate_number = n.plate_number
JOIN partners p ON p.code = 'DEMO-PARTNER-A'
ON CONFLICT (id) DO NOTHING;

INSERT INTO yard_vehicle_assignments (id, yard_id, vehicle_id, check_in_at, status)
SELECT a.id, y.id, v.id, CURRENT_TIMESTAMP - INTERVAL '30 minutes', 'IN_YARD'
FROM (VALUES
    ('a4000000-0000-0000-0000-000000000001'::uuid, 'DEMO-INYARD-01'),
    ('a4000000-0000-0000-0000-000000000002'::uuid, 'DEMO-INYARD-02')
) AS a(id, plate_number)
JOIN vehicles v ON v.plate_number = a.plate_number
CROSS JOIN yards y
WHERE y.code = 'DEMO-YARD-HN'
ON CONFLICT (id) DO NOTHING;

INSERT INTO yard_logs (id, pre_notification_id, vehicle_id, yard_id, check_in_at)
SELECT l.id, n.id, v.id, y.id, CURRENT_TIMESTAMP - INTERVAL '30 minutes'
FROM (VALUES
    ('a5000000-0000-0000-0000-000000000001'::uuid, 'a3000000-0000-0000-0000-000000000001'::uuid, 'DEMO-INYARD-01'),
    ('a5000000-0000-0000-0000-000000000002'::uuid, 'a3000000-0000-0000-0000-000000000002'::uuid, 'DEMO-INYARD-02')
) AS l(id, notification_id, plate_number)
JOIN pre_notifications n ON n.id = l.notification_id
JOIN vehicles v ON v.plate_number = l.plate_number
CROSS JOIN yards y
WHERE y.code = 'DEMO-YARD-HN'
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_orders (id, trip_id, vehicle_id, service_id, yard_id, status, start_at,
                            quantity, unit_price, total_amount)
SELECT so.id, t.id, v.id, s.id, y.id, 'IN_PROGRESS', CURRENT_TIMESTAMP - so.elapsed,
       0, s.hourly_rate, 0
FROM (VALUES
    ('a6000000-0000-0000-0000-000000000001'::uuid, 'a2000000-0000-0000-0000-000000000001'::uuid, 'DEMO-YARD-HOUR', INTERVAL '20 minutes'),
    ('a6000000-0000-0000-0000-000000000002'::uuid, 'a2000000-0000-0000-0000-000000000001'::uuid, 'DEMO-HANDLING', INTERVAL '15 minutes'),
    ('a6000000-0000-0000-0000-000000000003'::uuid, 'a2000000-0000-0000-0000-000000000002'::uuid, 'DEMO-YARD-HOUR', INTERVAL '25 minutes'),
    ('a6000000-0000-0000-0000-000000000004'::uuid, 'a2000000-0000-0000-0000-000000000002'::uuid, 'DEMO-MAINTENANCE', INTERVAL '10 minutes')
) AS so(id, trip_id, service_code, elapsed)
JOIN trips t ON t.id = so.trip_id
JOIN vehicles v ON v.id = t.vehicle_id
JOIN services s ON s.code = so.service_code
JOIN yards y ON y.code = 'DEMO-YARD-HN'
ON CONFLICT (id) DO NOTHING;
