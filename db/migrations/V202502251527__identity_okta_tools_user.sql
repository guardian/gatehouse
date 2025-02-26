CREATE USER identity_okta_tools;
GRANT SELECT ON users TO identity_okta_tools;

-- Run inside an anonymous function in order to use conditional logic such as IF
DO
$do$
    BEGIN
        -- The rds_iam role is created by the RDS IAM extension, which is not available in DEV
        IF EXISTS (select * from pg_roles where rolname='rds_iam') THEN
            GRANT rds_iam TO identity_okta_tools;
        END IF;
    END
$do$;