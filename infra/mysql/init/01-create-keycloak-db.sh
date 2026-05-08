#!/bin/sh
set -eu

require_safe_identifier() {
  value="$1"
  name="$2"

  case "$value" in
    *[!A-Za-z0-9_]*|'')
      echo "Invalid ${name}: only letters, digits, and underscore are allowed." >&2
      exit 1
      ;;
  esac
}

escape_sql_string() {
  printf "%s" "$1" | sed "s/'/''/g"
}

require_safe_identifier "${KC_DB_URL_DATABASE}" "KC_DB_URL_DATABASE"
require_safe_identifier "${KC_DB_USERNAME}" "KC_DB_USERNAME"

escaped_password="$(escape_sql_string "${KC_DB_PASSWORD}")"

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS \`${KC_DB_URL_DATABASE}\`;
CREATE USER IF NOT EXISTS '${KC_DB_USERNAME}'@'%' IDENTIFIED BY '${escaped_password}';
GRANT ALL PRIVILEGES ON \`${KC_DB_URL_DATABASE}\`.* TO '${KC_DB_USERNAME}'@'%';
FLUSH PRIVILEGES;
SQL
