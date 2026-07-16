-- Dev-only seed data. These are throwaway demo credentials (admin/admin, reader/reader) for local
-- runs and manual testing; do not reuse this migration's password hashes in any real environment.
INSERT INTO app_users (id, username, password_hash, roles, enabled) VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin', '$2y$10$.iWUBs.lE/M/DT1Acf7X0.ng.i0P6u5sKNHZG4ZHzqa2WkghHCnTu', 'ROLE_READ,ROLE_WRITE,ROLE_ADMIN', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'reader', '$2y$10$18DSTICIt9k5ueWPR0SIt.zu3almz2//tjvxmUV7zt82x93GzIa0C', 'ROLE_READ', TRUE);
