#!/bin/sh

if [ -n "${POSTGRES_USER_FILE:-}" ]; then
  POSTGRES_USER="$(cat "$POSTGRES_USER_FILE")"
fi

if [ -n "${POSTGRES_PASSWORD_FILE:-}" ]; then
  POSTGRES_PASSWORD="$(cat "$POSTGRES_PASSWORD_FILE")"
fi

if [ "$#" -gt 0 ]; then
  exec "$@"
fi

exec java -jar backend-0.0.1.jar
