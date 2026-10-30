CREATE TABLE user_permissions (
    user_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

INSERT INTO permissions (id, code, name, module) VALUES
    ('10000000-0000-0000-0000-000000000011', 'YARD_READ', 'View yards', 'YARD'),
    ('10000000-0000-0000-0000-000000000012', 'YARD_WRITE', 'Manage yards', 'YARD'),
    ('10000000-0000-0000-0000-000000000013', 'VEHICLE_OPERATE', 'Register vehicles', 'OPERATIONS'),
    ('10000000-0000-0000-0000-000000000014', 'GATE_OPERATE', 'Check vehicles in and out', 'OPERATIONS'),
    ('10000000-0000-0000-0000-000000000015', 'SERVICE_DISPATCH', 'Dispatch yard services', 'OPERATIONS'),
    ('10000000-0000-0000-0000-000000000016', 'TRACKING_READ', 'View GPS tracking', 'TRACKING'),
    ('10000000-0000-0000-0000-000000000017', 'TRACKING_WRITE', 'Manage GPS tracking', 'TRACKING'),
    ('10000000-0000-0000-0000-000000000018', 'FINANCE_READ', 'View invoices and debts', 'FINANCE'),
    ('10000000-0000-0000-0000-000000000019', 'FINANCE_WRITE', 'Manage invoices and payments', 'FINANCE'),
    ('10000000-0000-0000-0000-000000000020', 'REPORT_READ', 'View reports', 'REPORTS'),
    ('10000000-0000-0000-0000-000000000021', 'REPORT_WRITE', 'Manage reports', 'REPORTS'),
    ('10000000-0000-0000-0000-000000000022', 'GOODS_READ', 'View goods', 'GOODS'),
    ('10000000-0000-0000-0000-000000000023', 'GOODS_WRITE', 'Manage goods', 'GOODS'),
    ('10000000-0000-0000-0000-000000000024', 'CONFIRMATION_READ', 'View driver confirmations', 'CONFIRMATIONS'),
    ('10000000-0000-0000-0000-000000000025', 'CONFIRMATION_WRITE', 'Manage driver confirmations', 'CONFIRMATIONS')
ON CONFLICT (code) DO NOTHING;

-- Preserve role defaults while allowing admins to override them per user.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE (r.code = 'STAFF' AND p.code IN (
    'YARD_READ', 'VEHICLE_OPERATE', 'GATE_OPERATE', 'SERVICE_DISPATCH', 'TRACKING_READ',
    'FINANCE_READ', 'REPORT_READ', 'GOODS_READ', 'CONFIRMATION_READ'))
   OR (r.code = 'MANAGER' AND p.code IN (
    'YARD_READ', 'YARD_WRITE', 'VEHICLE_OPERATE', 'GATE_OPERATE', 'SERVICE_DISPATCH',
    'TRACKING_READ', 'TRACKING_WRITE', 'FINANCE_READ', 'FINANCE_WRITE', 'REPORT_READ',
    'REPORT_WRITE', 'GOODS_READ', 'GOODS_WRITE', 'CONFIRMATION_READ', 'CONFIRMATION_WRITE'))
   OR r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permission_id)
SELECT ur.user_id, rp.permission_id
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id AND r.code <> 'ADMIN'
JOIN role_permissions rp ON rp.role_id = ur.role_id
ON CONFLICT DO NOTHING;
