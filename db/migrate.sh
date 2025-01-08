#!/bin/sh

if [[ -z "${RDS_AUTH_TOKEN}" ]]; then
    RDS_AUTH_TOKEN=$(aws generate-db-auth-token \
        --hostname $RDS_HOSTNAME \
        --port $RDS_PORT \
        --region $RDS_REGION \
        --username $RDS_USERNAME
    )
fi

flyway migrate -user=$RDS_USERNAME -password=$RDS_AUTH_TOKEN -url=jdbc:postgresql://$RDS_HOSTNAME:$RDS_PORT/$RDS_DBNAME