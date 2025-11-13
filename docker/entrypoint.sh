#!/bin/sh

if [ $# -eq 0 ]; then
  echo "Missing command! Options are serve, staff, customer"
  exit 1
fi

if [ "$1" = "serve" ]; then
  echo "Waiting for IDP to be available at ${GOVER_HOSTNAME}/idp/realms/staff…"
  until curl --output /dev/null --silent --head --fail "${GOVER_HOSTNAME}/idp/realms/staff/"; do
    sleep 5
  done

  echo "Starting server…"
  java -jar /app/gover.jar
fi

if [ "$1" = "staff" ]; then
  echo "Waiting for the API to be available at ${GOVER_HOSTNAME}/api/public/actuator/health…"
  until curl --output /dev/null --silent --head --fail "${GOVER_HOSTNAME}/api/public/actuator/health"; do
    sleep 5
  done

  echo "Starting staff…"
  ln -sf /etc/nginx/sites-available/staff.conf /etc/nginx/sites-enabled/staff.conf
  nginx
fi

if [ "$1" = "customer" ]; then
  echo "Waiting for the API to be available at ${GOVER_HOSTNAME}/api/public/actuator/health…"
  until curl --output /dev/null --silent --head --fail "${GOVER_HOSTNAME}/api/public/actuator/health"; do
    sleep 5
  done

  echo "Starting customer…"
  ln -sf /etc/nginx/sites-available/customer.conf /etc/nginx/sites-enabled/customer.conf
  nginx
fi
