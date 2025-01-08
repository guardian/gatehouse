FROM amazon/aws-cli:2.22.28

RUN yum install -y curl tar gzip
RUN curl -sL https://download.red-gate.com/maven/release/com/redgate/flyway/flyway-commandline/11.1.0/flyway-commandline-11.1.0-linux-x64.tar.gz | tar xz

COPY ./sql /flyway/project/sql
COPY ./migrate.sh ./migrate.sh

ENTRYPOINT [ "./migrate.sh" ]