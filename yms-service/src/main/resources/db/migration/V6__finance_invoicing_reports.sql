ALTER TABLE payments ADD COLUMN proof_url VARCHAR(1000);
ALTER TABLE debts ADD COLUMN interest_accrued NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (interest_accrued >= 0);
ALTER TABLE debts ADD COLUMN interest_paid NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (interest_paid >= 0);
ALTER TABLE debts ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE debt_interest_configs (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL UNIQUE,
    rate_per_day_percent NUMERIC(8, 5) NOT NULL CHECK (rate_per_day_percent >= 0),
    grace_days INTEGER NOT NULL DEFAULT 0 CHECK (grace_days >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_debt_interest_partner FOREIGN KEY (partner_id) REFERENCES partners(id)
);

CREATE TABLE debt_interest_entries (
    id UUID PRIMARY KEY,
    debt_id UUID NOT NULL,
    accrual_date DATE NOT NULL,
    outstanding_principal NUMERIC(19, 4) NOT NULL,
    rate_per_day_percent NUMERIC(8, 5) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_debt_interest_entry_debt FOREIGN KEY (debt_id) REFERENCES debts(id),
    CONSTRAINT uq_debt_interest_day UNIQUE (debt_id, accrual_date)
);
CREATE INDEX idx_debt_interest_date ON debt_interest_entries(accrual_date, debt_id);

CREATE SEQUENCE invoice_number_seq START WITH 1 INCREMENT BY 1;
ALTER TABLE invoices ADD COLUMN invoice_type VARCHAR(20) NOT NULL DEFAULT 'SERVICE';
ALTER TABLE invoices ADD COLUMN e_invoice_provider_id VARCHAR(150);
ALTER TABLE invoices ADD COLUMN e_invoice_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SUBMITTED';
ALTER TABLE invoices ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_status_check;
ALTER TABLE invoices ADD CONSTRAINT invoices_status_check
    CHECK (status IN ('UNPAID', 'PAID', 'OVERDUE', 'CANCELLED'));

CREATE TABLE invoice_service_orders (
    invoice_id UUID NOT NULL,
    service_order_id UUID NOT NULL UNIQUE,
    line_amount NUMERIC(19, 4) NOT NULL CHECK (line_amount >= 0),
    PRIMARY KEY (invoice_id, service_order_id),
    CONSTRAINT fk_invoice_service_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_invoice_service_order FOREIGN KEY (service_order_id) REFERENCES service_orders(id)
);
CREATE INDEX idx_invoice_service_order_invoice ON invoice_service_orders(invoice_id);

CREATE INDEX idx_invoices_issue_date ON invoices(issue_date, partner_id);

CREATE TABLE report_schedules (
    id UUID PRIMARY KEY,
    report_type VARCHAR(20) NOT NULL CHECK (report_type IN ('MONTHLY', 'QUARTERLY')),
    recipient_email VARCHAR(254) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
