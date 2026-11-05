-- Repair legacy pre-notifications from the vehicle's registered Partner A.
UPDATE pre_notifications pn
SET partner_a_id = v.partner_a_id
FROM vehicles v
WHERE pn.vehicle_id = v.id
  AND pn.partner_a_id IS NULL;

ALTER TABLE pre_notifications
    ALTER COLUMN partner_a_id SET NOT NULL;
