-- Take a private_uuid, append the specified salt, and hash it to generate a unique external ID
CREATE FUNCTION gu_generate_identifier(private_id UUID, salt varchar) RETURNS varchar
   LANGUAGE SQL
   IMMUTABLE
   RETURNS NULL ON NULL INPUT
   RETURN encode(sha256((private_id || salt)::bytea ), 'hex' );

CREATE TABLE user_identifiers (
   id SERIAL PRIMARY KEY, 
   okta_id VARCHAR(100) UNIQUE NOT NULL,
   username varchar(20) UNIQUE,
   braze_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
   private_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(), 
   puzzle_id VARCHAR(100) UNIQUE NOT NULL GENERATED ALWAYS as (gu_generate_identifier(private_id, 'puzzle_id')) STORED,
   google_tag_id VARCHAR(100) UNIQUE NOT NULL GENERATED ALWAYS as (gu_generate_identifier(private_id, 'google_tag_id')) STORED
);

GRANT SELECT, INSERT, UPDATE, DELETE ON user_identifiers TO identity_api;