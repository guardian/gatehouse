CREATE USER identity_api;

-- Run inside an anonymous function in order to use conditional logic such as IF
DO
$do$
    BEGIN
        -- The rds_iam role is created by the RDS IAM extension, which is not available in DEV
        IF EXISTS (select * from pg_roles where rolname='rds_iam') THEN
            GRANT rds_iam TO identity_api;
        END IF;
    END
$do$;