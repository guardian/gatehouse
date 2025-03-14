# Gatehouse

This [Play Framework](https://www.playframework.com/) app provides an API for managing Identity accounts.
The API forms a thin wrapper around our Identity Provider.

While it's in a state of transition, it's also used to manage user service and marketing permissions.

## Structure

The main components of the app are:

```mermaid
classDiagram
    class UserController
    class UserService
    class CompositeUserService
    class OktaUserService
    class legacyIdentityDbUserService

    UserController <--> UserService: User
    UserService <|-- CompositeUserService
    CompositeUserService --* OktaUserService
    CompositeUserService --* legacyIdentityDbUserService

    UserService: healthCheck()
    UserService: fetchUserByIdentityId()
    UserService: ...()
```

## Running the app locally

1. Set up a [local PostgreSQL service](https://github.com/guardian/identity-platform/tree/main/postgres) listening on port 5555.
2. Install local AWS credentials for the Identity account.
3. Set the `AWS_PROFILE` environment variable to `identity`:
   ```bash  
   export AWS_PROFILE=identity
   ```
4. Run the app:
   ```
   sbt run
   ```

## Configuration

The app configuration has three levels of precedence. Any values repeated at a higher level will override those at a
lower
level.  
The levels are, in order of precedence:

### 1. SSM parameters

Secret and private settings are stored as
[AWS SSM parameters](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html),
named in the format:  
`/<stage>/identity/gatehouse/<param key>`   
where `param key` is a slash-separated
[Hocon](https://github.com/lightbend/config/blob/main/HOCON.md) key.

Eg.  
`/CODE/identity/gatehouse/play/http/secret/key`  
would give us a `play.http.secret.key` value.

Secrets are stored as `SecureString` parameters.  
Private settings are stored as `String` parameters.

### 2. Stage-specific settings

Settings that aren't private but vary between deployment stages are set in the stage-specific config files in
the `conf` directory. These might be values that we expose in browsers, for example.

### 3. Global settings

Finally, settings that aren't private and are the same for all deployment stages are set in the `conf/application.conf`
file.

### Okta-specific configuration

TODO: Instructions on how to generate an Okta access token locally.     
We use the Okta Code org locally because this makes it easier to develop and test with users that have already been
registered in [Code gateway](https://profile.code.dev-theguardian.com/).

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