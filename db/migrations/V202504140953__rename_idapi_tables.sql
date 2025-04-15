--
-- Rename tables used exclusively by Identity API to be prefixed with `idapi`
--

ALTER TABLE clients RENAME TO idapi_clients;
ALTER TABLE client_access_tokens RENAME TO idapi_client_access_tokens;
