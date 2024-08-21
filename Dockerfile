FROM node:22.2-alpine as build_app

ARG buildDate=unknown
ARG version=0.0.0

# Build Mails

WORKDIR /mails

COPY mails/package.json package.json
COPY mails/package-lock.json package-lock.json

# Install mail dependencies
RUN npm install

COPY mails/partials partials
COPY mails/templates templates

# Build mails
RUN npm run build:prod

# Build Staff and Customer

WORKDIR /app

# Copy frontend project files
COPY app/package.json package.json
COPY app/package-lock.json package-lock.json

# Set version in package.json
RUN sed -i 's/"version": "X.X.X"/"version": "'"$version"'"/g' package.json

# Install frontend dependencies
RUN npm install

# Copy frontend source files
COPY app/public public
COPY app/src src
COPY app/tsconfig.json tsconfig.json

# Set build date in app-config.ts
RUN sed -i "s|2000-01-01T00:00:00.000Z|$buildDate|g" src/app-config.ts

# Build staff app
ENV NODE_ENV production
ENV REACT_APP_BUILD_TARGET staff
ENV PUBLIC_URL /staff
ENV DISABLE_ESLINT_PLUGIN true

RUN npm run build
RUN mv build staff

# Build customer app
ENV NODE_ENV production
ENV REACT_APP_BUILD_TARGET customer
ENV PUBLIC_URL /
ENV DISABLE_ESLINT_PLUGIN true

RUN npm run build
RUN mv build customer


# Build server
FROM  maven:3.9.6-eclipse-temurin-21 as build_server

ARG version=0.0.0

# Prepare backend working directoy
WORKDIR /app

# Copy backend project files
COPY pom.xml pom.xml

# Set version in pom.xml
RUN sed -i 's%<version>X.X.X</version>%<version>'"$version"'</version>%g' pom.xml

# Copy backend source files
COPY src/main src/main

# Copy mails files
COPY --from=build_app /mails/dist src/main/resources/templates/mail

# Build app
RUN mvn install -DskipTests

# App
FROM eclipse-temurin:21.0.2_13-jre

ARG buildDate=unknown
ARG version=0.0.0
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
ENV TZ "Europe/Berlin"
ENV LANG de_DE.UTF-8
ENV LANGUAGE de_DE:de
ENV LC_ALL de_DE.UTF-8

# Copy nginx configs
COPY docker/nginx.staff.conf /etc/nginx/sites-available/staff.conf
COPY docker/nginx.customer.conf /etc/nginx/sites-available/customer.conf

# Copy entrypoint script
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Prepare app working directoy
WORKDIR /app

# Install locale dependencies
RUN apt-get update && \
    apt-get install -y tzdata locales nginx && \
    ln -fs /usr/share/zoneinfo/Europe/Berlin /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata

# Remove default nginx config
RUN rm /etc/nginx/sites-enabled/default

# Disable nginx daemon mode
RUN echo "daemon off;" >> /etc/nginx/nginx.conf

# Copy app files
COPY --from=build_server /app/target/Gover-$version.jar /app/gover.jar
COPY --from=build_app /app/staff /var/www/staff/html
COPY --from=build_app /app/customer /var/www/customer/html

ENTRYPOINT ["/app/entrypoint.sh"]
