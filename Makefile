.PHONY: dev dev-down setup run-backend run-frontend build push deploy undeploy logs-backend logs-frontend

REGISTRY ?= localhost:5001
TAG      ?= latest

BACKEND_IMAGE  = $(REGISTRY)/bravo-backend:$(TAG)
FRONTEND_IMAGE = $(REGISTRY)/bravo-frontend:$(TAG)

dev:
	docker compose up -d

dev-down:
	docker compose down -v

setup:
	cp -n apps/backend/.env.example apps/backend/.env

run-backend:
	cd apps/backend && ./mvnw spring-boot:run

run-frontend:
	cd apps/frontend && npm run dev

build:
	docker build -t $(BACKEND_IMAGE)  apps/backend/
	docker build -t $(FRONTEND_IMAGE) apps/frontend/

push: build
	docker push $(BACKEND_IMAGE)
	docker push $(FRONTEND_IMAGE)

deploy:
	kubectl apply -f infra/k8s/namespace.yaml
	kubectl apply -f infra/k8s/postgres/
	kubectl apply -f infra/k8s/redis/
	kubectl apply -f infra/k8s/backend/
	kubectl apply -f infra/k8s/frontend/
	kubectl apply -f infra/k8s/ingress.yaml

undeploy:
	kubectl delete -f infra/k8s/ --recursive --ignore-not-found

logs-backend:
	kubectl logs -n bravo -l app=backend -f

logs-frontend:
	kubectl logs -n bravo -l app=frontend -f
