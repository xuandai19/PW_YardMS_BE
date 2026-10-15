CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- Identity / RBAC
-- =========================================================
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    module VARCHAR(50) NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- =========================================================
-- Partners / Contracts
-- =========================================================
CREATE TABLE partners (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    tax_code VARCHAR(50),
    contact_person VARCHAR(150),
    contact_phone VARCHAR(30),
    contact_email VARCHAR(150),
    address VARCHAR(500),
    country_code VARCHAR(10) NOT NULL,
    partner_type VARCHAR(10) NOT NULL CHECK (partner_type IN ('A', 'C')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE partner_relationships (
    partner_a_id UUID NOT NULL,
    partner_c_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (partner_a_id, partner_c_id),
    CONSTRAINT fk_partner_rel_a FOREIGN KEY (partner_a_id) REFERENCES partners(id),
    CONSTRAINT fk_partner_rel_c FOREIGN KEY (partner_c_id) REFERENCES partners(id),
    CONSTRAINT chk_partner_rel_distinct CHECK (partner_a_id <> partner_c_id)
);

CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    contract_no VARCHAR(80) NOT NULL UNIQUE,
    contract_type VARCHAR(10) NOT NULL CHECK (contract_type IN ('A-B', 'A-C')),
    partner_a_id UUID NOT NULL,
    partner_c_id UUID,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    contract_value NUMERIC(19, 4) NOT NULL CHECK (contract_value >= 0),
    file_url VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_a FOREIGN KEY (partner_a_id) REFERENCES partners(id),
    CONSTRAINT fk_contract_c FOREIGN KEY (partner_c_id) REFERENCES partners(id),
    CONSTRAINT chk_contract_date CHECK (end_date >= start_date),
    CONSTRAINT chk_contract_party CHECK (
        (contract_type = 'A-B' AND partner_c_id IS NULL)
        OR (contract_type = 'A-C' AND partner_c_id IS NOT NULL)
    )
);

-- =========================================================
-- Vehicle / Yard / Service
-- =========================================================
CREATE TABLE yards (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    plate_number VARCHAR(30) NOT NULL,
    partner_a_id UUID NOT NULL,
    carrier_company VARCHAR(255),
    vehicle_type VARCHAR(80),
    driver_name VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_partner_a FOREIGN KEY (partner_a_id) REFERENCES partners(id)
);

CREATE TABLE services (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    service_type VARCHAR(30) NOT NULL CHECK (service_type IN ('YARD', 'HANDLING', 'MAINTENANCE', 'FUEL', 'OTHER')),
    unit VARCHAR(20) NOT NULL DEFAULT 'HOUR',
    hourly_rate NUMERIC(19, 4) NOT NULL CHECK (hourly_rate >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service_price_history (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    price NUMERIC(19, 4) NOT NULL CHECK (price >= 0),
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    changed_by UUID,
    CONSTRAINT fk_price_history_service FOREIGN KEY (service_id) REFERENCES services(id),
    CONSTRAINT fk_price_history_user FOREIGN KEY (changed_by) REFERENCES users(id),
    CONSTRAINT chk_price_history_dates CHECK (valid_to IS NULL OR valid_to > valid_from)
);

CREATE TABLE trips (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    planned_at TIMESTAMPTZ,
    departed_at TIMESTAMPTZ,
    arrived_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trip_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
);

CREATE TABLE service_orders (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    service_id UUID NOT NULL,
    yard_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    quantity NUMERIC(19, 4) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price NUMERIC(19, 4) NOT NULL CHECK (unit_price >= 0),
    total_amount NUMERIC(19, 4) NOT NULL CHECK (total_amount >= 0),
    assigned_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_order_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT fk_service_order_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_service_order_service FOREIGN KEY (service_id) REFERENCES services(id),
    CONSTRAINT fk_service_order_yard FOREIGN KEY (yard_id) REFERENCES yards(id),
    CONSTRAINT fk_service_order_user FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT chk_service_order_dates CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at)
);

CREATE TABLE yard_vehicle_assignments (
    id UUID PRIMARY KEY,
    yard_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    check_in_at TIMESTAMPTZ NOT NULL,
    check_out_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_YARD',
    CONSTRAINT fk_yva_yard FOREIGN KEY (yard_id) REFERENCES yards(id),
    CONSTRAINT fk_yva_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT chk_yva_dates CHECK (check_out_at IS NULL OR check_out_at >= check_in_at)
);

-- =========================================================
-- Goods / QR / Gate
-- =========================================================
CREATE TABLE goods (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    item_code VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    quantity NUMERIC(19, 4) NOT NULL CHECK (quantity >= 0),
    unit VARCHAR(30) NOT NULL,
    received_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (received_quantity >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'EXPECTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goods_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT chk_goods_received CHECK (received_quantity <= quantity)
);

CREATE TABLE qr_codes (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(30) NOT NULL CHECK (entity_type IN ('VEHICLE', 'GOODS')),
    entity_id UUID NOT NULL,
    qr_value VARCHAR(500) NOT NULL UNIQUE,
    checksum VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_qr_expiry CHECK (expires_at > issued_at)
);

CREATE TABLE gate_events (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    yard_id UUID,
    qr_code_id UUID,
    event_type VARCHAR(20) NOT NULL CHECK (event_type IN ('CHECK_IN', 'CHECK_OUT', 'REJECTED')),
    event_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by UUID,
    reason VARCHAR(500),
    CONSTRAINT fk_gate_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_gate_yard FOREIGN KEY (yard_id) REFERENCES yards(id),
    CONSTRAINT fk_gate_qr FOREIGN KEY (qr_code_id) REFERENCES qr_codes(id),
    CONSTRAINT fk_gate_user FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE TABLE route_logs (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    speed_kmh NUMERIC(8, 2),
    heading NUMERIC(6, 2),
    accuracy_m NUMERIC(8, 2),
    source VARCHAR(30) NOT NULL DEFAULT 'GPS',
    off_route BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_route_trip FOREIGN KEY (trip_id) REFERENCES trips(id)
);

-- =========================================================
-- Finance
-- =========================================================
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    invoice_no VARCHAR(80) NOT NULL UNIQUE,
    partner_id UUID NOT NULL,
    contract_id UUID,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal NUMERIC(19, 4) NOT NULL CHECK (subtotal >= 0),
    tax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total_amount NUMERIC(19, 4) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID', 'OVERDUE', 'CANCELLED')),
    pdf_url VARCHAR(1000),
    issued_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_partner FOREIGN KEY (partner_id) REFERENCES partners(id),
    CONSTRAINT fk_invoice_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT chk_invoice_dates CHECK (due_date >= issue_date)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    paid_at TIMESTAMPTZ NOT NULL,
    method VARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER',
    reference_no VARCHAR(100),
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

CREATE TABLE debts (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL,
    invoice_id UUID NOT NULL UNIQUE,
    principal_amount NUMERIC(19, 4) NOT NULL CHECK (principal_amount >= 0),
    paid_amount NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    interest_rate_per_day NUMERIC(8, 5) NOT NULL DEFAULT 0 CHECK (interest_rate_per_day >= 0),
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_debt_partner FOREIGN KEY (partner_id) REFERENCES partners(id),
    CONSTRAINT fk_debt_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT chk_debt_paid CHECK (paid_amount <= principal_amount)
);

-- =========================================================
-- HR / Equipment
-- =========================================================
CREATE TABLE departments (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employees (
    id UUID PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    position VARCHAR(100),
    department_id UUID,
    assigned_yard_id UUID,
    shift_code VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_employee_yard FOREIGN KEY (assigned_yard_id) REFERENCES yards(id)
);

CREATE TABLE equipments (
    id UUID PRIMARY KEY,
    asset_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    equipment_type VARCHAR(100) NOT NULL,
    model VARCHAR(100),
    purchase_date DATE,
    service_interval_days INTEGER CHECK (service_interval_days > 0),
    assigned_yard_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_equipment_yard FOREIGN KEY (assigned_yard_id) REFERENCES yards(id)
);

CREATE TABLE maintenances (
    id UUID PRIMARY KEY,
    equipment_id UUID NOT NULL,
    maintenance_date DATE NOT NULL,
    next_due_date DATE,
    description VARCHAR(1000),
    cost NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    performed_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES equipments(id),
    CONSTRAINT fk_maintenance_employee FOREIGN KEY (performed_by) REFERENCES employees(id),
    CONSTRAINT chk_maintenance_dates CHECK (next_due_date IS NULL OR next_due_date >= maintenance_date)
);

-- =========================================================
-- Audit
-- =========================================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_username VARCHAR(80) NOT NULL,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_path VARCHAR(255),
    duration_ms BIGINT,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_partners_type ON partners(partner_type);
CREATE INDEX idx_contracts_partner_a ON contracts(partner_a_id);
CREATE INDEX idx_contracts_end_date ON contracts(end_date);
CREATE INDEX idx_vehicles_partner_a ON vehicles(partner_a_id);
CREATE UNIQUE INDEX idx_vehicles_plate ON vehicles (plate_number, id);
CREATE INDEX idx_yards_status ON yards(status);
CREATE INDEX idx_service_orders_trip ON service_orders(trip_id);
CREATE INDEX idx_service_orders_vehicle_status ON service_orders(vehicle_id, status);
CREATE INDEX idx_trips_vehicle ON trips(vehicle_id);
CREATE INDEX idx_route_logs_trip_recorded ON route_logs(trip_id, recorded_at DESC);
CREATE INDEX idx_goods_trip ON goods(trip_id);
CREATE INDEX idx_qr_entity ON qr_codes(entity_type, entity_id);
CREATE INDEX idx_gate_events_vehicle_time ON gate_events(vehicle_id, event_at DESC);
CREATE INDEX idx_invoices_partner_status ON invoices(partner_id, status);
CREATE INDEX idx_payments_invoice_paid_at ON payments(invoice_id, paid_at DESC);
CREATE INDEX idx_debts_partner_status ON debts(partner_id, status);
CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_equipment_yard ON equipments(assigned_yard_id);
CREATE INDEX idx_maintenance_equipment_date ON maintenances(equipment_id, maintenance_date DESC);
CREATE INDEX idx_audit_actor_time ON audit_logs(actor_username, created_at DESC);
CREATE INDEX idx_audit_module_time ON audit_logs(module, created_at DESC);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at DESC);

-- Append-only audit protection.
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only';
END;
$$;

CREATE TRIGGER trg_audit_logs_no_update
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();
