CREATE TABLE app_users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    roles         VARCHAR(255) NOT NULL, -- comma-separated role names, e.g. "ROLE_READ,ROLE_WRITE"
    enabled       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX ux_app_users_username ON app_users (username);
