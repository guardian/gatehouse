#!/bin/bash

# Launch a Postgres test container and run the migrations against it.

White=$"\033[37m"
Red='\033[31m'
GreenBold='\033[1;32m'
Yellow='\033[33m'
Reset='\033[0m'

# postgres:16.6
POSTGRES_CONTAINER="postgres@sha256:c965017e1d29eb03e18a11abc25f5e3cd78cb5ac799d495922264b8489d5a3a1"

# Exit on error
set -e

echo -e "${White}Starting test database container...${Reset}"

# Run postgres and clean it up when the script exits
CONTAINER_ID=$(docker run \
    --rm -d -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=gatehouse -p 6543:5432 ${POSTGRES_CONTAINER}
)
cleanup() {
    echo -e "${White}Stopping test database container...${Reset}"
    docker stop ${CONTAINER_ID}
}
trap "cleanup" EXIT

echo -e "${White}Postgres Container ID: ${Yellow}${CONTAINER_ID}${Reset}"
echo -e "${White}Waiting 10 seconds for database to warm up.${Reset}"
sleep 10

$(dirname "$0")/migrate.sh DEV true

echo -e "${GreenBold}Tests passed!${Reset}"