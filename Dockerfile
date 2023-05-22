##
## Build
##
FROM --platform=linux/amd64 eclipse-temurin:17-alpine as build

RUN apk add --update git nodejs npm

##
## Copy App
##
WORKDIR /gover/app

COPY app/.npmrc .npmrc
COPY app/package.json package.json
COPY app/package-lock.json package-lock.json

RUN npm ci

COPY app/public public
COPY app/scripts scripts
COPY app/src src
COPY app/tsconfig.json tsconfig.json

##
## Copy Server
##
WORKDIR /gover

COPY .git .git
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml

COPY src src

##
## Build
##
WORKDIR /gover
RUN ./mvnw -DskipTests install


##
## Run
##
FROM --platform=linux/amd64 eclipse-temurin:17 as run

ENV TZ "Europe/Berlin"
ENV LANG de_DE.UTF-8
ENV LANGUAGE de_DE:de
ENV LC_ALL de_DE.UTF-8

WORKDIR /app

RUN apt-get update && \
    apt-get -y install tzdata locales && \
    wget -O /tmp/wkhtmltox.deb https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6.1-2/wkhtmltox_0.12.6.1-2.jammy_amd64.deb && \
    apt-get install -y -f /tmp/wkhtmltox.deb && \
    rm /tmp/wkhtmltox.deb && \
    ln -fs /usr/share/zoneinfo/Europe/Berlin /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata

COPY --from=build /gover/target/Gover-2.1.8.jar /app/gover.jar

ENTRYPOINT ["java"]
CMD  ["-jar", "/app/gover.jar"]
