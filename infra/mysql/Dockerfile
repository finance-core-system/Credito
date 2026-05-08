FROM mysql:8.4

COPY init/01-create-keycloak-db.sh /docker-entrypoint-initdb.d/01-create-keycloak-db.sh

RUN chmod 755 /docker-entrypoint-initdb.d/01-create-keycloak-db.sh
