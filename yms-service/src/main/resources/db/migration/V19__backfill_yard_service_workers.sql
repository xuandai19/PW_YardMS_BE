-- Backfill the sample roster for active yards that were added after V11 or
-- did not have workers for a service type when the original seed ran.
INSERT INTO yard_service_workers (id, yard_id, service_type, worker_name, active)
SELECT (substr(md5(y.id::text || ':' || s.service_type || ':' || worker.number::text), 1, 8) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':' || worker.number::text), 9, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':' || worker.number::text), 13, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':' || worker.number::text), 17, 4) || '-' ||
        substr(md5(y.id::text || ':' || s.service_type || ':' || worker.number::text), 21, 12))::uuid,
       y.id, s.service_type,
       'Nhân viên ' || s.service_type || ' 0' || worker.number::text,
       TRUE
FROM yards y
CROSS JOIN (SELECT DISTINCT service_type FROM services WHERE active = TRUE) s
CROSS JOIN (VALUES (1), (2)) AS worker(number)
WHERE y.status = 'ACTIVE'
ON CONFLICT (yard_id, service_type, worker_name) DO NOTHING;
