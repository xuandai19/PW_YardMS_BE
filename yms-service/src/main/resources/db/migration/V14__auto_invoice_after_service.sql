ALTER TABLE invoices ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SENT';
ALTER TABLE invoices ADD COLUMN delivery_error VARCHAR(500);
ALTER TABLE invoices ADD COLUMN delivered_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_invoice_service_order ON invoice_service_orders(service_order_id);
