#!/bin/sh

mode="$(cat /app/runtime-mode 2>/dev/null || echo app)"

if [ "$mode" = "serve" ]; then
  # IPv6 addresses must be set in brackets to work in this URL.
  curl --output /dev/null --silent --fail http://${PROSUNA_MANAGEMENT_SERVER_ADDRESS:-127.0.0.1}:${PROSUNA_MANAGEMENT_SERVER_PORT:-9090}/api/actuator/health
else
  curl --output /dev/null --silent --fail http://127.0.0.1:8080/
fi
