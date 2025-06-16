FROM node:24.2.0-alpine3.21 AS build_mails

# Set working directory
WORKDIR /mails

# Copy mail project files
COPY mails .

# Install mail dependencies
RUN npm install

# Build mails
RUN npm run build:prod

FROM node:24.2.0-alpine3.21 AS build_app

# Set build version and date
ARG version=0.0.0
ARG buildDate=unknown

# Set work dir
WORKDIR /app

# Copy frontend source files
COPY app .

# Install frontend dependencies
RUN npm install

# Set version in package.json
RUN sed -i 's/"version": "X.X.X"/"version": "'"$version"'"/g' package.json

# Set build date in app-config.ts
RUN sed -i "s|2000-01-01T00:00:00.000Z|$buildDate|g" src/app-config.ts

# Build staff app
RUN npm run build:staff

# Build customer app
RUN npm run build:customer

FROM  maven:3.9.9-eclipse-temurin-21-alpine AS build_server

# Set build version and date
ARG version=0.0.0
ARG buildDate=unknown

# Prepare backend working directoy
WORKDIR /app

# Copy backend project files
COPY pom.xml pom.xml

# Set version in pom.xml
RUN sed -i 's%<version>X.X.X</version>%<version>'"$version"'</version>%g' pom.xml

# Copy backend source files
COPY src/main src/main

# Copy mails files
COPY --from=build_mails /mails/dist src/main/resources/templates/mail

# Build app
RUN mvn install -DskipTests

# App
FROM eclipse-temurin:21.0.7_6-jre-alpine-3.21

ARG version=0.0.0
ARG buildDate=unknown
ARG TARGETARCH

# Set app metadata
LABEL org.opencontainers.image.created=$buildDate
LABEL org.opencontainers.image.url="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.documentation="https://wiki.teamaivot.de/de/dokumentation/gover"
LABEL org.opencontainers.image.source="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.version=$version
LABEL org.opencontainers.image.vendor="Aivot"
LABEL org.opencontainers.image.title="Gover Backend"
LABEL org.opencontainers.image.description="Gover is an efficient low-code e-government platform for creating and managing user-centric online forms."

# Set locale env config
ENV TZ="Europe/Berlin"
ENV LANG=de_DE.UTF-8
ENV LANGUAGE=de_DE:de
ENV LC_ALL=de_DE.UTF-8

# Copy nginx configs
COPY docker/nginx.staff.conf /etc/nginx/sites-available/staff.conf
COPY docker/nginx.customer.conf /etc/nginx/sites-available/customer.conf

# Copy entrypoint script
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Prepare app working directoy
WORKDIR /app

# Install locale dependencies
RUN apk upgrade --no-cache && \
    apk add tzdata musl musl-utils musl-locales nginx

# Remove default nginx config
RUN rm /etc/nginx/http.d/default.conf

# Disable nginx daemon mode
RUN echo "daemon off;" >> /etc/nginx/nginx.conf

# Copy app files
COPY --from=build_server /app/target/Gover-$version.jar /app/gover.jar
COPY --from=build_app /app/build/staff /var/www/staff/html
COPY --from=build_app /app/build/customer /var/www/customer/html

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["customer"]