STACK_NAME=nosql1

BACKEND_DOCKERFILE=./backend/Dockerfile 
BACKEND_IMAGE_NAME=nosql-lab1-backend
BACKEND_PATH=./backend

FRONTEND_DOCKERFILE=./nginx/Dockerfile
FRONTEND_IMAGE_NAME=nosql-lab1-frontend
FRONTEND_PATH=.  # это плохо, очень плохо. как и всё что сделано в этом проекте

DEV_COMPOSE=docker-compose.dev.yaml
PROD_COMPOSE=docker-compose.yaml
all: prod

build-backend:
	docker buildx build \
		-f $(BACKEND_DOCKERFILE) \
		-t $(BACKEND_IMAGE_NAME) $(BACKEND_PATH)

build-frontend:
	docker buildx build \
		-f $(FRONTEND_DOCKERFILE) \
		-t $(FRONTEND_IMAGE_NAME) $(FRONTEND_PATH)

dev: build-backend build-frontend
	docker compose -f ${DEV_COMPOSE} up --force-recreate

prod: build-backend build-frontend
	docker stack deploy \
		-c ${PROD_COMPOSE} \
		$(STACK_NAME)

stop:
	docker stack rm $(STACK_NAME)

migrate-firestore: build-backend
	docker compose -f $(DEV_COMPOSE) run --rm backend \
		java -jar backend-0.0.1.jar --spring.profiles.active=firestore-migration
