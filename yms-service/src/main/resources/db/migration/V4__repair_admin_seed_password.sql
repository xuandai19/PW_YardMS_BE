-- The original seed hash was generated for a different password.
-- Repair only the untouched bootstrap credential; preserve any user-managed password.
UPDATE users
SET password_hash = '$2a$10$I.sIVmE7jTicSZS5tB0EN.LThWgTP6g7aLmE/9VNsoo.2aSs.k7su',
    failed_login_attempts = 0,
    locked_until = NULL
WHERE username = 'admin'
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
