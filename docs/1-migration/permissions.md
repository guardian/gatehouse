# Permissions migration from Identity to Gatehouse

## Objectives

Currently User permissions(originally known as Consents) are stored in the `jdoc` JSON column along with various other data points.

  - **Have a defined user schema**


    JSON columns can contain any arbitrary JSON object meaning that some users may have data in their user objects that Identity API might not even know about. For example, if a field previously existed, but was eventually removed and a backfill was never perfomed to remove the now unused data. 

  - **Reduce the risk of backfills**

    Since most user data is stored in one column, a slight mistake updating permissions in the query could accidentally overwrite adjacent data. This coupled with the unintuitiveness of JSON operations in SQL makes backfilling permission data a dangerous operation to perform on the database.

  - **Prevent race conditions**

    As most user data is stored in one column, any updates to said column have to compete with eachother for priority. The way IDAPI functions is that it first retrieves the JSON data from Postgres, mutates it in code, and then runs an update to insert the updated JSON, the problem with this approach is that if another thread is updating the same user at the same time, theres potential for the update to get lost. We noticed this scenario happening quite frequently when migrating the `lastActiveDate` data to Gatehouse.


## Proposed Schema

| Name          | Type      | Constraints           | Default | Notes                                                                                                       |
|---------------|-----------|-----------------------|---------|-------------------------------------------------------------------------------------------------------------|
| user_id       | VARCHAR   | NOT NULL, PRIMARY KEY |         | Identity ID                                                                                                 |
| permission_id | VARCHAR   | NOT NULL, PRIMARY KEY |         | Permission ID (eg 'similar_guardian_products')                                                              |
| enabled     | BOOLEAN   | NOT NULL              |         | Is the permission enabled?                                                                                  |
| last_modified | TIMESTAMP | NOT NULL              |      | The last time the permission status was changed                                                             |
| actor         | VARCHAR   | NOT NULL              |         | What was responsible for the permission change, usually "user" but may be different for backfilled consents |

## Considerations

### Keeping un-used data

Currently we're not aware of any usecases for the `last_modified` column, and then `actor` column. `last_modified` could be used to indicate how long a user has permitted a certain permission, but in practice we rely on a daily versioned table of the data lake to track this data. `actor` is also similarly un-used, it might be useful at some point if we were ever asked why a user has a certain permission, but right now its not propogated to the datalake so any such analysis would need to be done adhoc by the Identity team.

For the time being we've decided to retain as much data as possible for the sake of simplicity, and avoiding deleting data we may need at some point. As we're now storing all of this data in individual SQL columns its fairly trivial to delete the data at a later point.

### `permissions` table

Currently all available permissions are stored as code in Identity API, we could potentially create a third `permissions` table which just contains a list of the possible permissions. This would mean the table only has a dozen or so rows and would likely only consist of 2 columns `permission_id` and `permission_name`.

Functionally this would have the benefit of making renaming a permission quite easy, we'd simply just update the 1 row containing the permission name, although renaming a permission this way would likely break Identity API as it would expect the permission to have a certain name.

There is another benefit where we could have a foreign key on the `permission_id` of the `users_permissions` table, allowing us to automatically delete all of a users permission choices when a permission is deleted. Either way, a developer needs to directly access the database and run a `DELETE` on the `permissisons` or the `user_permissions` table.

### Refactoring opt in permissions

Right now we have a standard of suffixing opt in permissions with `_optin` in the current Identity DB, and they're stored in exactly the same way as regular permissions. This has the downside, which we've had issues with multiple times, where the default value for these permissions is `false`, meaning that whenever the user is created they're already automatically opted in to said permissions.

Ordinarily this is fine, as users would immediately see their permission choices in the registration flow and get the chance to change their permissions, however if the user for some reason doesn't see that page, for example if they click off the registration journey, or we have a bug where the page isn't shown, they remain opted into to the permission despite not being given a choice.

This is to a degree, not just an issue with how we store permissions, but how we handle permissions in general, and fixing this is going to require some discussion with data privacy and significant code changes in IDAPI, so for the time being we're not tackling this issue and opting to move permissions as is.

## Lessons from the last migration

The last migration we did relied on using Amazons Data Migration Service (DMS) to make a carbon copy of the Identity DB `users` table in Gatehouse DB. We then relied on an
**INSERT/SELECT** to move data from the Identity DB `users` table format, to the new Gatehouse DB `users` table format. At the time there was very little load on Gatehouse DB so any approach for moving the data would have worked fairly well.

However as we moved data from the legacy table to the new table we noticed that it caused a lot of locks, causing Identity API queries to start timing out and causing 500 errors. Ideally instead of doing one big **INSERT/SELECT** to move the data over we should try and limit how many users we're updating at a time to avoid causing any issues in Identity API.

One proposal on how we might improve the data migration might be to piggyback off the work we've already done in the Datalake export job to compare data between the two database.
At the moment the Datalake export job compares the data for each user in Identity DB to its counterpart in Gatehouse DB and records any mismatches, we'll need to do this again to track the Permissions migration status, but in addition to that we could also add a little bit of logic to not only compare the data, but also fix the data in Gatehouse if it detects a mismatch. 

In order to limit how many user updates are happening at once we could limit the datalake export to only fix a certain % of mismatches every day, for example out of every 100 mismatches it will only fix 1 of them, ideally at this point Identity API should already be updating and inserting data in the Gatehouse DB table meaning that over time the number of mismatches will gradually reduce to 0.


## Migration plan

 - [x] Plan new `users_permissions` schema
 - [x] Signoff from DPO on `permissions` naming
 - [ ] Deploy the new `users_permissions` table using Flyway
 - [ ] Modify the datalake export job to **SELECT** Permissions from Gatehouse DB and compare them to Identity DB, recording total matches/mismatches
 - [ ] **DELETE** Permissions from Gatehouse DB from Identity API (realistically this should be done automatically when the user is deleted thanks to the foreign key)
 - [ ] **UPSERT** Permissions to Gatehouse DB from Identity API
 - [ ] **UPSERT** Permissions to Gatehouse DB from Datalake export. Initially starting with only a small % of users.
 - [ ] Wait for mismatches to gradually reduce to 0% or close to.
 - [ ] **SELECT** Permissions from Gatehouse DB from Identity API
 - [ ] Remove references to Identity DB from Identity API
 - [ ] Switch off Identity DB

## Outstanding questions

 - How do we handle adding a new permissions? 
 - Should every user have a row for each permission, or do we only have rows for each permission a user has made a choice for?