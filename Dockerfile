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
ARG BUILD_VERSION=0.0.0
ARG BUILD_NUMBER=0
ARG BUILD_DATE=2025-05-24T10:15:00Z

# Set work dir
WORKDIR /app

# Copy frontend source files
COPY app .

# Set build version and date
RUN sed -i 's/@buildVersion/'"$BUILD_VERSION"'/g' /app/src/app-info.ts && \
    sed -i 's/@buildNumber/'"$BUILD_NUMBER"'/g' /app/src/app-info.ts && \
    sed -i 's/@buildTimestamp/'"$BUILD_DATE"'/g' /app/src/app-info.ts

# Install frontend dependencies
RUN npm install

# Build staff app
RUN npm run build:staff

# Build customer app
RUN npm run build:customer

FROM  maven:3.9.9-eclipse-temurin-21-alpine AS build_server

# Set build version and date
ARG BUILD_VERSION=0.0.0
ARG BUILD_NUMBER=0
ARG BUILD_DATE=2025-05-24T10:15:00Z

# Prepare backend working directoy
WORKDIR /app

# Copy backend project files
COPY pom.xml pom.xml

# Copy backend source files
COPY src/main src/main

# Set build version and date
RUN sed -i 's/@buildVersion/'"$BUILD_VERSION"'/g' /app/src/main/resources/application.yml && \
    sed -i 's/@buildNumber/'"$BUILD_NUMBER"'/g' /app/src/main/resources/application.yml && \
    sed -i 's/@buildTimestamp/'"$BUILD_DATE"'/g' /app/src/main/resources/application.yml

# Copy mails files
COPY --from=build_mails /mails/dist src/main/resources/templates/mail

# Build app
RUN mvn install -DskipTests

# App
FROM eclipse-temurin:21.0.10_7-jre-alpine-3.21

# Set build version and date
ARG BUILD_VERSION=0.0.0
ARG BUILD_NUMBER=0
ARG BUILD_DATE=2025-05-24T10:15:00Z

# Set app metadata
LABEL org.opencontainers.image.created=$BUILD_DATE
LABEL org.opencontainers.image.url="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.documentation="https://wiki.teamaivot.de/de/dokumentation/gover"
LABEL org.opencontainers.image.source="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.version=$BUILD_VERSION
LABEL org.opencontainers.image.vendor="Aivot"
LABEL org.opencontainers.image.title="Gover Backend"
LABEL org.opencontainers.image.description="Gover is an efficient low-code e-government platform for creating and managing user-centric online forms."

# Set locale env config
ENV TZ="Europe/Berlin"
ENV LANG=de_DE.UTF-8
ENV LANGUAGE=de_DE:de
ENV LC_ALL=de_DE.UTF-8

ENV BUILD_VERSION=$BUILD_VERSION
ENV BUILD_NUMBER=$BUILD_NUMBER
ENV BUILD_DATE=$BUILD_DATE

# Prepare app working directoy
WORKDIR /app

# Copy entrypoint script
COPY docker/entrypoint.sh /app/entrypoint.sh

# Install locale, nginx, configure nginx and entrypoint script
RUN apk upgrade --no-cache && \
    apk add curl tzdata musl musl-utils musl-locales nginx && \
    chmod +x /app/entrypoint.sh

# Copy nginx configs
COPY docker/nginx.conf /etc/nginx/http.d/default.conf

# Copy app files
COPY --from=build_server /app/target/gover-backend-0.0.0.jar /app/gover.jar
COPY --from=build_app /app/build/customer /app/www
COPY --from=build_app /app/build/staff /app/www/staff

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["app"]