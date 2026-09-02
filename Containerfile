FROM docker.io/library/node:26.7.0-alpine3.23 AS build_mails

# Set working directory
WORKDIR /mails

# Copy mail project files
COPY backend/mails .

# Install mail dependencies
RUN npm install

# Build mails
RUN npm run build:prod

FROM docker.io/library/node:26.7.0-alpine3.23 AS build_app

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

# Copy backend permission providers for generated frontend permissions
COPY backend/src/main/java /backend/src/main/java

# Build staff app
RUN npm run build:staff

# Build customer app
RUN npm run build:customer

FROM  docker.io/library/maven:3.9.16-eclipse-temurin-25-alpine AS build_server

# Set build version and date
ARG BUILD_VERSION=0.0.0
ARG BUILD_NUMBER=0
ARG BUILD_DATE=2025-05-24T10:15:00Z

# Prepare backend working directoy
WORKDIR /app

# Copy backend project files
COPY backend/pom.xml pom.xml

# Copy backend source files
COPY backend/src/main src/main

# Set build version and date
RUN sed -i 's/@buildVersion/'"$BUILD_VERSION"'/g' /app/src/main/resources/application.yml && \
    sed -i 's/@buildNumber/'"$BUILD_NUMBER"'/g' /app/src/main/resources/application.yml && \
    sed -i 's/@buildTimestamp/'"$BUILD_DATE"'/g' /app/src/main/resources/application.yml

# Copy mails files
COPY --from=build_mails /mails/dist src/main/resources/templates/mail

# Build app
RUN mvn install -DskipTests

# App
FROM docker.io/library/eclipse-temurin:25.0.3_9-jre-alpine-3.23

# Set build version and date
ARG BUILD_VERSION=0.0.0
ARG BUILD_NUMBER=0
ARG BUILD_DATE=2025-05-24T10:15:00Z

# Set app metadata
LABEL org.opencontainers.image.created=$BUILD_DATE
LABEL org.opencontainers.image.url="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.documentation="https://docs.prosuna.de"
LABEL org.opencontainers.image.source="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.version=$BUILD_VERSION
LABEL org.opencontainers.image.vendor="Aivot"
LABEL org.opencontainers.image.title="Prosuna Backend"
LABEL org.opencontainers.image.description="Prosuna is an efficient low-code e-government platform for creating and managing user-centric online forms."

# Set locale env config
ENV LANG=de_DE.UTF-8
ENV LANGUAGE=de_DE:de
ENV LC_ALL=de_DE.UTF-8

ENV BUILD_VERSION=$BUILD_VERSION
ENV BUILD_NUMBER=$BUILD_NUMBER
ENV BUILD_DATE=$BUILD_DATE

# Prepare app working directoy
WORKDIR /app

# Copy entrypoint and healthcheck scripts
COPY container/entrypoint.sh /app/entrypoint.sh
COPY container/healthcheck.sh /app/healthcheck.sh

# Install locale, nginx, configure nginx and entrypoint script
RUN apk upgrade --no-cache && \
    apk add curl tzdata musl musl-utils musl-locales nginx && \
    chmod +x /app/entrypoint.sh /app/healthcheck.sh && \
    mkdir -p /app/default-assets

# Copy nginx configs
COPY container/nginx.conf /etc/nginx/http.d/default.conf

# Copy default assets
COPY default-assets /app/default-assets

# Copy app files
COPY --from=build_server /app/target/backend-${BUILD_VERSION}-exec.jar /app/prosuna.jar
COPY --from=build_app /app/build/customer /app/www
COPY --from=build_app /app/build/staff /app/www/staff
# Keep the SBOM separate from both frontend build outputs and expose one canonical bundle.
COPY build/sbom /app/www/sbom
RUN rm -f /app/www/sbom/.gitkeep

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD ["/app/healthcheck.sh"]

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["app"]
