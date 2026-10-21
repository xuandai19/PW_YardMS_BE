-- Scheduled/assigned work has no measured duration until it is completed.
-- Keep quantity at zero before completion; end() writes the measured quantity.
ALTER TABLE service_orders
    DROP CONSTRAINT IF EXISTS service_orders_quantity_check;

ALTER TABLE service_orders
    ADD CONSTRAINT chk_service_order_quantity_nonnegative CHECK (quantity >= 0);
