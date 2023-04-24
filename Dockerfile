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

WORKDIR /app

RUN apt-get update
RUN apt-get -y install wkhtmltopdf

RUN ln -s /usr/bin/wkhtmltopdf /usr/local/bin/wkhtmltopdf;
RUN chmod +x /usr/local/bin/wkhtmltopdf;

COPY --from=build /gover/target/Gover-2.1.6.jar /app/gover.jar

ENTRYPOINT ["java"]
CMD  ["-jar", "/app/gover.jar"]
