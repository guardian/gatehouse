CREATE TABLE clients 
(
    id TEXT PRIMARY KEY NOT NULL,
    scopes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    created TIMESTAMP DEFAULT now() NOT NULL,
    updated TIMESTAMP DEFAULT now() NOT NULL
);

CREATE TABLE client_access_tokens
(
    token TEXT NOT NULL PRIMARY KEY,
    client_id TEXT NOT NULL REFERENCES clients,
    created TIMESTAMP DEFAULT now() NOT NULL,
    expiry TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT true NOT NULL
);

GRANT SELECT ON clients TO identity_api;
GRANT SELECT ON client_access_tokens TO identity_api;

GRANT INSERT, SELECT, UPDATE, DELETE ON clients TO identity_admin_api;
GRANT INSERT, SELECT, UPDATE, DELETE ON client_access_tokens TO identity_admin_api;