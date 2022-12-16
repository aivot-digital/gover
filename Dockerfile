##
## Build Frontend
##
FROM node:17-alpine AS build-frontend

RUN apk add git

WORKDIR /gover-frontend

COPY ./gover-frontend/package.json ./
COPY ./gover-frontend/package-lock.json ./
COPY ./gover-frontend/.npmrc ./

RUN npm install

COPY ./gover-frontend/ ./
COPY ./.git /.git

RUN npm run build:app
RUN mv ./build ./app

RUN PUBLIC_URL="/admin/" npm run build:admin
RUN mv ./build ./admin

##
## Build Master
##
FROM nginx:alpine

WORKDIR /app

RUN apk add openjdk17 \
            wkhtmltopdf \
            xvfb \
            ttf-dejavu ttf-droid ttf-freefont ttf-liberation

RUN ln -s /usr/bin/wkhtmltopdf /usr/local/bin/wkhtmltopdf;
RUN chmod +x /usr/local/bin/wkhtmltopdf;

COPY ./gover-backend/.mvn/ ./.mvn/
COPY ./gover-backend/mvnw ./
COPY ./gover-backend/pom.xml ./

COPY ./gover-backend/src ./src/

RUN /app/mvnw -DskipTests install

COPY --from=build-frontend /gover-frontend/app /var/www/app/html
COPY --from=build-frontend /gover-frontend/admin /var/www/admin/html

COPY ./nginx.conf /etc/nginx/conf.d/default.conf

ENTRYPOINT ["/bin/sh"]
CMD  ["-c", "nginx & /app/mvnw spring-boot:run"]
