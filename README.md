# Gatehouse

As of 16/03/2026 this repository only contains the new Gatehouse DB and its associated schema. This repository previously contained a Scala app that was due to replace Identity API, however plans to replace Identity API have been put on hold whilst we upgrade its database.

The last commit which included the Gatehouse scala app can be found [here](https://github.com/guardian/gatehouse/tree/92d138b48ba355ade6e642821487f7fec774a906)

## Database schema migrations

This repo contains the schema files for the Gatehouse database, these can be found in the [`./db`](./db/) folder.

### Creating a new database schema migration

The following steps require `docker`, `aws`, and `jq` installed.

1. Create a new file in [`./db/migrations`](./db/migrations/) with the naming scheme `V(YYYYMMDD)__Migration_Description.sql`. The naming scheme is important and the migration will be skipped if it does not adhere to it, take note of the double underscore between the version number and migration description.
2. Run `./db/test.sh` to test and verify your new migration against the local test database.
3. Create a PR and merge your new migration into `main`
4. Run `./db/migrate.sh CODE` to apply the migrations to CODE.
   - You will be prompted to rotate the admin user credentials, we suggest you always do this but you may want to delay the rotation if you are worried about your migration causing any outages.
5. Run `./db/migrate.sh PROD` to apply the migrations to PROD. 