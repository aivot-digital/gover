#!/bin/sh

mode="$(cat /app/runtime-mode 2>/dev/null || echo app)"

if [ "$mode" = "serve" ]; then
  curl --output /dev/null --silent --fail http://127.0.0.1:8080/api/actuator/health
else
  curl --output /dev/null --silent --fail http://127.0.0.1/
fi
