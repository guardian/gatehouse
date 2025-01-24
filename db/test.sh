#!/bin/bash

# Set working directory to this scripts folder.
cd "$(dirname "$0")"

White=$"\033[37m"
RedBold='\033[1;31m'
GreenBold='\033[1;32m'
Reset='\033[0m'

echo -e "${White}Running migrations on test Postgres DB...${Reset}"

docker compose up --abort-on-container-exit --exit-code-from migrate
MIGRATION_EXIT_CODE=$?

# Clean up database
docker compose down

if [[ $MIGRATION_EXIT_CODE -ne 0 ]]; then
    echo -e "${RedBold}Migrations failed! See above.${Reset}"
    exit 1
else
    echo -e "${GreenBold}Tests passed!${Reset}"
fi