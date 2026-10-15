-- Foundation seed data for M1 Identity / RBAC
-- Password hash is BCrypt for: Admin@123

INSERT INTO roles (id, code, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'STAFF', 'Staff'),
    ('00000000-0000-0000-0000-000000000002', 'MANAGER', 'Manager'),
    ('00000000-0000-0000-0000-000000000003', 'ADMIN', 'Administrator')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (id, code, name, module) VALUES
    ('10000000-0000-0000-0000-000000000001', 'AUTH_LOGIN', 'Login', 'AUTH'),
    ('10000000-0000-0000-0000-000000000002', 'USER_READ', 'View users', 'M1'),
    ('10000000-0000-0000-0000-000000000003', 'USER_WRITE', 'Manage users', 'M1'),
    ('10000000-0000-0000-0000-000000000004', 'PARTNER_READ', 'View partners', 'M2'),
    ('10000000-0000-0000-0000-000000000005', 'PARTNER_WRITE', 'Manage partners', 'M2'),
    ('10000000-0000-0000-0000-000000000006', 'CONTRACT_READ', 'View contracts', 'M3'),
    ('10000000-0000-0000-0000-000000000007', 'CONTRACT_WRITE', 'Manage contracts', 'M3'),
    ('10000000-0000-0000-0000-000000000008', 'SERVICE_READ', 'View services', 'M7'),
    ('10000000-0000-0000-0000-000000000009', 'SERVICE_WRITE', 'Manage services', 'M7'),
    ('10000000-0000-0000-0000-000000000010', 'AUDIT_READ', 'View audit logs', 'AUDIT')
ON CONFLICT (code) DO NOTHING;

-- STAFF permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions
WHERE code IN ('AUTH_LOGIN', 'PARTNER_READ', 'CONTRACT_READ', 'SERVICE_READ')
ON CONFLICT DO NOTHING;

-- MANAGER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permissions
WHERE code IN ('AUTH_LOGIN', 'PARTNER_READ', 'PARTNER_WRITE', 'CONTRACT_READ', 'CONTRACT_WRITE',
               'SERVICE_READ', 'SERVICE_WRITE', 'AUDIT_READ')
ON CONFLICT DO NOTHING;

-- ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000003', id FROM permissions
ON CONFLICT DO NOTHING;

-- Default admin user (password Admin@123). Hash generated with BCrypt strength 10.
-- DataInitializer will create admin if missing; this seed is optional bootstrap.
INSERT INTO users (id, username, password_hash, enabled)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;
