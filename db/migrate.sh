#!/bin/bash

export AWS_REGION=eu-west-1
export AWS_PROFILE=identity

# flyway/flyway:11-alpine
FLYWAY_CONTAINER="flyway/flyway@sha256:1850b2e6b257f774cdd6ad7554dc53cc278351ff0202f7b9b696ceafccbea493"

Grey=$"\033[30m"
Red='\033[31m'
GreenBold='\033[1;32m'
Yellow='\033[33m'
White=$"\033[37m"
WhiteBold=$"\033[1;37m"
Reset='\033[0m'

STAGE=$1
APPLY_MIGRATIONS=$2

# Applying migrations from a non-mainline branch will mess up the database schema
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "${BRANCH}" != "main" ]]; then
  echo './migrate.sh can only be ran on main branch.'
  exit 1
fi

case $STAGE in
    CODE)
        ;;
    PROD)
        echo -e "${Red}--------------------------------------\n"
        echo -e "WARNING: You are about to run this script on the PROD environment. Are you sure? ${Yellow}[y/N]${Red}\n"
        echo -e "${Red}--------------------------------------${Reset}"
        read -r input
        if [[ "${input}" != [Yy] ]]; then
            echo "Aborting."
            exit 1
        fi
        ;;
    *)
        echo "Usage: ${0} {CODE|PROD}"
        exit 1
        ;;
esac

echo -e "${White}Starting schema migrations on ${Yellow}${STAGE}${Reset}"

DB_CLUSTER_IDENTIFIER=$(
    aws ssm get-parameter \
        --name "/$STAGE/identity/gatehouse/db-identifier" \
        --query "Parameter.Value" \
        --output text
)
if [[ -z "${DB_CLUSTER_IDENTIFIER}" ]]; then
    echo "Failed to retrieve database secret ARN from SSM."
    exit 1
fi

echo -e "${White}Resolved DB Cluster as: ${Yellow}${DB_CLUSTER_IDENTIFIER}${Reset}"

DB_WRITER_ENDPOINT=$(
    aws rds describe-db-clusters \
        --db-cluster-identifier "${DB_CLUSTER_IDENTIFIER}" \
        --query "DBClusters[0].Endpoint" \
        --output text
)
if [[ -z "${DB_WRITER_ENDPOINT}" ]]; then
    echo "Failed to retrieve writer endpoint for the database cluster."
    exit 1
fi

echo -e "${White}Resolved Writer endpoint as: ${Yellow}${DB_WRITER_ENDPOINT}${Reset}"

DB_SECRET_ARN=$(
    aws rds describe-db-clusters \
        --db-cluster-identifier "${DB_CLUSTER_IDENTIFIER}" \
        --query "DBClusters[0].MasterUserSecret.SecretArn" \
        --output text
)
if [[ -z "${DB_WRITER_ENDPOINT}" ]]; then
    echo "Failed to retrieve writer endpoint for the database cluster."
    exit 1
fi

echo -e "${White}Resolved Master user secret ARN as: ${Yellow}${DB_SECRET_ARN}${Reset}"

DB_CREDENTIALS=$(
    aws secretsmanager get-secret-value \
        --secret-id "${DB_SECRET_ARN}" \
        --query "SecretString" \
        --output text
)
if [[ -z "${DB_CREDENTIALS}" ]]; then
    echo "Failed to retrieve database credentials from Secrets Manager."
    exit 1
fi

DB_USERNAME=$(
    echo "${DB_CREDENTIALS}" | jq -r ".username"
)
DB_PASSWORD=$(
    echo "${DB_CREDENTIALS}" | jq -r ".password"
)

echo -e "${White}Starting SSH tunnel to writer endpoint...${Clear}"

# Start SSH session in foreground and fork to background after connection is established
SSH_TUNNEL_COMMAND="$(ssm ssh --raw -t identity-psql-client,${STAGE},identity 2>/dev/null) \
    -o ExitOnForwardFailure=yes -fN -L 6543:${DB_WRITER_ENDPOINT}:5432"

echo -e "${White}Executing SSH tunnel command:\n\n${Grey}${SSH_TUNNEL_COMMAND}${Clear}\n"

# Slightly hacky but couldn't get SSH ControlMaster to work with the AWS session-manager-plugin
# Terminate the SSH connection when the script exits as SSH doesn't seem to be able to clean up
# AWS's session-manager-plugin properly.
cleanup_ssh_tunnel() { kill $(pgrep -f session-manager-plugin); }
trap "cleanup_ssh_tunnel" EXIT

eval "$SSH_TUNNEL_COMMAND"
echo -e "${White}SSH tunnel open, Database available on ${Yellow}127.0.0.1:6543${White}.${Reset}"

echo -e "${White}Starting migration...${Grey}\n"

LOCALHOST='host.docker.internal'
if [[ ! -z "$CI" ]]; then
    # When running in Github Actions docker doesn't have docker.host.internal as a valid hostname
    # Likely as it doesn't need to run the container in a VM unlike local development on MacOS
    LOCALHOST="172.17.0.1"
fi

# Check pending migrations
FLYWAY_OPTS="-url=jdbc:postgresql://${LOCALHOST}:6543/gatehouse -user=${DB_USERNAME} -password=${DB_PASSWORD} -locations=filesystem:./migrations"
docker run --net host --rm -v $(dirname "$0")/migrations:/flyway/migrations ${FLYWAY_CONTAINER} \
    info ${FLYWAY_OPTS}

if [[ "$?" != "0" ]]; then
    echo -e "${Red}Database migration failed.${Reset}"
    exit 1
fi

echo -e "${WhiteBold}Apply pending migrations? ${Yellow}[y/N]${Reset}${Grey}"

read -r input
if [[ "${input}" != [Yy] ]]; then
    echo "Aborting."
    exit 1
fi

echo ""

# Apply database migrations
docker run --net host --rm -v $(dirname "$0")/migrations:/flyway/migrations ${FLYWAY_CONTAINER} \
    migrate ${FLYWAY_OPTS}

echo ""

if [[ "$?" != "0" ]]; then
    echo -e "${Red}Database migration failed.${Reset}"
    exit 1
else
    echo -e "${GreenBold}Database migration for ${STAGE} completed successfully.${Reset}"
fi

# Rotate admin user credentials
echo -e "${WhiteBold}Rotate Admin Credentials?${Reset}"
echo -e "${White}Rotating admin credentials will take a few minutes and break this script until it has completed.${Reset}\n"
echo -e "${White}You can also trigger the secret rotation manually using the following command:${Grey}\n\naws secretsmanager rotate-secret --secret-id ${DB_SECRET_ARN} --profile identity --region eu-west-1\n"
echo -e "${White}Rotate admin user credentials now? ${Yellow}[Y/n]${Reset}\n"

read -r input
if [[ "${input}" == [Nn] ]]; then
    exit 1
fi

echo -e "${White}Rotating admin user credentials.${Reset}"  

aws secretsmanager rotate-secret \
    --secret-id "${DB_SECRET_ARN}"

echo -e "${White}Done, new credentials will take a few minutes to take effect.${Reset}"  