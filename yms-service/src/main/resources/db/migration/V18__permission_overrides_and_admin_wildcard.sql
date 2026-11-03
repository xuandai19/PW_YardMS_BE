ALTER TABLE user_permissions
    ADD COLUMN effect VARCHAR(10) NOT NULL DEFAULT 'GRANT';

ALTER TABLE user_permissions
    ADD CONSTRAINT chk_user_permission_effect CHECK (effect IN ('GRANT', 'DENY'));

-- Existing role-derived grants should remain inherited from roles.
DELETE FROM user_permissions up
USING user_roles ur, role_permissions rp
WHERE up.user_id = ur.user_id
  AND rp.role_id = ur.role_id
  AND up.permission_id = rp.permission_id;

INSERT INTO permissions (id, code, name, module)
VALUES ('10000000-0000-0000-0000-000000000026', '*', 'Full system access', 'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
