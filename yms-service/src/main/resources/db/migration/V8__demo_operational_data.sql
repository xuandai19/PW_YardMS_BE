-- Linked, deterministic sample records for the implemented UI modules.
-- Uses reserved DEMO codes and unique business keys so the migration is safe to retry.

INSERT INTO partners (id, code, name, country_code, partner_type, tax_code, contact_person, contact_phone, contact_email, address)
VALUES
    ('81000000-0000-0000-0000-000000000001', 'DEMO-PARTNER-A', 'Công ty Vận tải Mẫu', 'VN', 'A', '0100000001', 'Nguyễn An', '0900000001', 'a.demo@example.test', 'Hà Nội'),
    ('81000000-0000-0000-0000-000000000002', 'DEMO-PARTNER-C', 'Công ty Kho vận Mẫu', 'VN', 'C', '0100000002', 'Trần Bình', '0900000002', 'c.demo@example.test', 'Hải Phòng')
ON CONFLICT (code) DO NOTHING;

INSERT INTO partner_relationships (partner_a_id, partner_c_id)
SELECT a.id, c.id FROM partners a CROSS JOIN partners c
WHERE a.code = 'DEMO-PARTNER-A' AND c.code = 'DEMO-PARTNER-C'
ON CONFLICT DO NOTHING;

INSERT INTO contracts (id, contract_no, contract_type, partner_a_id, partner_c_id, start_date, end_date, contract_value, status)
SELECT '82000000-0000-0000-0000-000000000001', 'DEMO-CONTRACT-AC', 'A-C', a.id, c.id,
       CURRENT_DATE - 30, CURRENT_DATE + 335, 250000000, 'ACTIVE'
FROM partners a CROSS JOIN partners c
WHERE a.code = 'DEMO-PARTNER-A' AND c.code = 'DEMO-PARTNER-C'
ON CONFLICT (contract_no) DO NOTHING;

INSERT INTO yards (id, code, name, address, latitude, longitude, capacity, status)
VALUES ('83000000-0000-0000-0000-000000000001', 'DEMO-YARD-HN', 'Bãi mẫu Hà Nội', 'Long Biên, Hà Nội', 21.028511, 105.804817, 80, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO services (id, code, name, service_type, unit, hourly_rate, active)
VALUES
    ('84000000-0000-0000-0000-000000000001', 'DEMO-YARD-HOUR', 'Lưu bãi theo giờ (mẫu)', 'YARD', 'HOUR', 50000, TRUE),
    ('84000000-0000-0000-0000-000000000002', 'DEMO-HANDLING', 'Bốc xếp hàng (mẫu)', 'HANDLING', 'TON', 150000, TRUE),
    ('84000000-0000-0000-0000-000000000003', 'DEMO-MAINTENANCE', 'Kiểm tra phương tiện (mẫu)', 'MAINTENANCE', 'JOB', 200000, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO vehicles (id, plate_number, partner_a_id, carrier_company, vehicle_type, driver_name, status)
SELECT '85000000-0000-0000-0000-000000000001', 'DEMO-30A-00001', id, 'Vận tải Mẫu', 'Truck', 'Lê Minh', 'REGISTERED'
FROM partners WHERE code = 'DEMO-PARTNER-A'
ON CONFLICT (id) DO NOTHING;

INSERT INTO trips (id, vehicle_id, origin, destination, planned_at, status)
SELECT '86000000-0000-0000-0000-000000000001', id, 'Bãi mẫu Hà Nội', 'Kho vận mẫu Hải Phòng', CURRENT_TIMESTAMP + INTERVAL '6 hours', 'PLANNED'
FROM vehicles WHERE plate_number = 'DEMO-30A-00001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO pre_notifications (id, vehicle_id, partner_a_id, trip_id, plate_number, arrival_date, planned_arrival,
                              driver_name, driver_phone, carrier_company, vehicle_type, status)
SELECT '87000000-0000-0000-0000-000000000001', v.id, p.id, t.id, v.plate_number, CURRENT_DATE,
       CURRENT_TIMESTAMP + INTERVAL '6 hours', 'Lê Minh', '0900000003', 'Vận tải Mẫu', 'Truck', 'REGISTERED'
FROM vehicles v JOIN partners p ON p.code = 'DEMO-PARTNER-A'
JOIN trips t ON t.id = '86000000-0000-0000-0000-000000000001'
WHERE v.plate_number = 'DEMO-30A-00001'
ON CONFLICT (plate_number, arrival_date) DO NOTHING;

INSERT INTO service_orders (id, trip_id, vehicle_id, service_id, yard_id, status, start_at, end_at, quantity, unit_price, total_amount)
SELECT '88000000-0000-0000-0000-000000000001', t.id, v.id, s.id, y.id, 'COMPLETED',
       CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours', 2, 50000, 100000
FROM trips t JOIN vehicles v ON v.id = t.vehicle_id
JOIN services s ON s.code = 'DEMO-YARD-HOUR' JOIN yards y ON y.code = 'DEMO-YARD-HN'
WHERE t.id = '86000000-0000-0000-0000-000000000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO goods (id, trip_id, item_code, description, quantity, unit, received_quantity, status, qr_value)
SELECT '89000000-0000-0000-0000-000000000001', id, 'DEMO-GOODS-001', 'Linh kiện đóng kiện (dữ liệu mẫu)', 12, 'kiện', 10, 'DISCREPANCY', 'YMS-DEMO-GOODS-001'
FROM trips WHERE id = '86000000-0000-0000-0000-000000000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO goods_discrepancies (id, goods_id, expected_quantity, scanned_quantity, message, resolved)
VALUES ('8a000000-0000-0000-0000-000000000001', '89000000-0000-0000-0000-000000000001', 12, 10,
        'Thiếu 2 kiện so với số lượng dự kiến (dữ liệu mẫu)', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO driver_confirmations (id, trip_id, confirmation_type, status, requested_at, due_at)
VALUES
    ('8b000000-0000-0000-0000-000000000001', '86000000-0000-0000-0000-000000000001', 'DEPARTURE', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 minutes'),
    ('8b000000-0000-0000-0000-000000000002', '86000000-0000-0000-0000-000000000001', 'ARRIVAL', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '4 hours')
ON CONFLICT (trip_id, confirmation_type) DO NOTHING;

INSERT INTO route_logs (id, trip_id, recorded_at, latitude, longitude, speed_kmh, heading, accuracy_m, source, off_route)
VALUES
    ('8c000000-0000-0000-0000-000000000001', '86000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '20 minutes', 21.028511, 105.804817, 0, 90, 8, 'DEMO', FALSE),
    ('8c000000-0000-0000-0000-000000000002', '86000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '10 minutes', 21.039000, 105.820000, 35, 90, 8, 'DEMO', FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO invoices (id, invoice_no, partner_id, contract_id, issue_date, due_date, subtotal, tax_amount,
                      total_amount, status, invoice_type)
SELECT '8d000000-0000-0000-0000-000000000001', 'DEMO-INV-0001', p.id, c.id,
       CURRENT_DATE, CURRENT_DATE + 30, 100000, 10000, 110000, 'UNPAID', 'SERVICE'
FROM partners p JOIN contracts c ON c.contract_no = 'DEMO-CONTRACT-AC'
WHERE p.code = 'DEMO-PARTNER-C'
ON CONFLICT (invoice_no) DO NOTHING;

INSERT INTO invoice_service_orders (invoice_id, service_order_id, line_amount)
SELECT i.id, so.id, so.total_amount FROM invoices i CROSS JOIN service_orders so
WHERE i.invoice_no = 'DEMO-INV-0001' AND so.id = '88000000-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;

INSERT INTO debts (id, partner_id, invoice_id, principal_amount, paid_amount, interest_rate_per_day, due_date, status)
SELECT '8e000000-0000-0000-0000-000000000001', i.partner_id, i.id, i.total_amount, 0, 0.01, i.due_date, 'OPEN'
FROM invoices i WHERE i.invoice_no = 'DEMO-INV-0001'
ON CONFLICT (invoice_id) DO NOTHING;
