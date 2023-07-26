# Build cli
FROM --platform=linux/amd64 golang:1.20-alpine as build_cli

# Prepare working directoy
WORKDIR /gover/cli

# Prepare go env config
ENV GOOS linux
ENV GOARCH amd64

# Copy cli files
COPY cli/go.mod go.mod
COPY cli/go.sum go.sum
COPY cli/cmd cmd
COPY cli/src src

# Fetch dependencies
RUN go mod download

# Build cli tools
RUN go build -o ./create_admin cmd/create_admin/main.go
RUN go build -o ./delete_submissions cmd/delete_submissions/main.go
RUN go build -o ./fill_test_data cmd/fill_test_data/main.go


# Build app
FROM --platform=linux/amd64 eclipse-temurin:17-alpine as build_app

# Install dependencies
RUN apk add --update \
    git \
    nodejs \
    npm

# Prepare frontend working directoy
WORKDIR /gover/app

# Copy frontend project files
COPY app/.npmrc .npmrc
COPY app/package.json package.json
COPY app/package-lock.json package-lock.json

# Install frontend dependencies
RUN npm ci

# Copy frontend source files
COPY app/public public
COPY app/scripts scripts
COPY app/src src
COPY app/tsconfig.json tsconfig.json

# Prepare backend working directoy
WORKDIR /gover

# Copy git information
COPY .git .git

# Copy backend project files
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml

# Copy backend source files
COPY src src


# Build app
WORKDIR /gover
RUN ./mvnw -DskipTests install


# App
FROM --platform=linux/amd64 eclipse-temurin:17 as app

ARG buildDate
ARG version

# Set app metadata
LABEL org.opencontainers.image.created=$buildDate
LABEL org.opencontainers.image.url="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.documentation="https://wiki.teamaivot.de/de/dokumentation/gover"
LABEL org.opencontainers.image.source="https://github.com/aivot-digital/gover"
LABEL org.opencontainers.image.version=$version
LABEL org.opencontainers.image.vendor="Aivot"
LABEL org.opencontainers.image.title="Gover"
LABEL org.opencontainers.image.description="Gover is an efficient low-code e-government platform for creating and managing user-centric online applications."

# Set locale env config
ENV TZ "Europe/Berlin"
ENV LANG de_DE.UTF-8
ENV LANGUAGE de_DE:de
ENV LC_ALL de_DE.UTF-8

# Prepare app working directoy
WORKDIR /app

# Install locale dependencies
RUN apt-get update && \
    apt-get -y install tzdata locales cron && \
    wget -O /tmp/wkhtmltox.deb https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6.1-2/wkhtmltox_0.12.6.1-2.jammy_amd64.deb && \
    apt-get install -y -f /tmp/wkhtmltox.deb && \
    rm /tmp/wkhtmltox.deb && \
    ln -fs /usr/share/zoneinfo/Europe/Berlin /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata

# Setup cron job
RUN crontab -l | { cat; echo "0 0 * * * /app/delete_submissions"; } | crontab -

# Copy cli files
COPY --from=build_cli /gover/cli/create_admin /app/create_admin
COPY --from=build_cli /gover/cli/delete_submissions /app/delete_submissions
COPY --from=build_cli /gover/cli/fill_test_data /app/fill_test_data

# Copy app files
COPY --from=build_app /gover/target/Gover-2.1.8.jar /app/gover.jar

ENTRYPOINT ["java"]
CMD  ["-jar", "/app/gover.jar"]
