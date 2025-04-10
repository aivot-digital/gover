#!/bin/sh

if [ $# -eq 0 ]
  then
    echo "Missing command! Options are serve, staff, customer"
    exit 1
fi

if [ "$1" = "serve" ]
  then
    echo "Starting server..."
    java -jar /app/gover.jar
fi

if [ "$1" = "staff" ]
  then
    echo "Starting staff..."
    ln -sf /etc/nginx/sites-available/staff.conf /etc/nginx/sites-enabled/staff.conf
    nginx
fi

if [ "$1" = "customer" ]
  then
    echo "Starting customer..."
    ln -sf /etc/nginx/sites-available/customer.conf /etc/nginx/sites-enabled/customer.conf
    nginx
fi