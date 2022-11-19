all: out frontend backend

out:
	rm -rf ./out
	mkdir ./out
	mkdir ./out/html

frontend:
	cd ./gover-frontend; \
	npm install; \
	npm run build:app; \
	mv ./build ../out/html/app; \
	PUBLIC_URL="/admin/" npm run build:admin; \
	mv ./build ../out/html/admin

backend:
	cd ./gover-backend; \
	./mvnw -DskipTests clean install; \
	mv ./target/GoverBackend-0.0.1-SNAPSHOT.jar ../out/gover.jar
