# Task Manager API

A Spring Boot REST API for managing tasks, backed by PostgreSQL, 
containerized with Docker, and deployed to Amazon EKS (Fargate) 
behind an AWS Application Load Balancer.

## Features
- Full CRUD for tasks: create, read, update, delete
- Input validation with clear error responses
- Task status enum: `PENDING`, `IN_PROGRESS`, `COMPLETED`
- Auto-generated API docs via Swagger UI
- Health checks and Prometheus metrics exposed via Spring Boot Actuator
  (used in the companion monitoring project)

## Tech Stack
- **Backend:** Java 17, Spring Boot 3, Spring Data JPA
- **Database:** PostgreSQL
- **Containerization:** Docker (multi-stage build), Docker Compose
- **Deployment:** Amazon EKS (Fargate), Kubernetes, AWS Load Balancer Controller
- **Docs:** springdoc-openapi (Swagger UI)
- **Observability:** Spring Boot Actuator + Micrometer (Prometheus format)

## API Endpoints

| Method | Endpoint          | Description        |
|--------|-------------------|---------------------|
| GET    | /api/tasks        | List all tasks      |
| GET    | /api/tasks/{id}   | Get a task by ID    |
| POST   | /api/tasks        | Create a new task   |
| PUT    | /api/tasks/{id}   | Update a task       |
| DELETE | /api/tasks/{id}   | Delete a task       |

Swagger UI available at: `/swagger-ui.html`
Health check: `/actuator/health`
Prometheus metrics: `/actuator/prometheus`

## Running Locally

### Option A: Docker Compose (easiest — app + DB together)
```bash
docker compose up --build
```
App will be available at `http://localhost:8080`.

### Option B: Maven + local PostgreSQL
1. Start a local PostgreSQL instance with a `taskdb` database.
2. Run:
   ```bash
   mvn spring-boot:run
   ```

## Deploying to Amazon EKS

This project reuses the EKS cluster (`democluster`, AWS Fargate) and the 
AWS Load Balancer Controller set up in my [eks-app-deployment](../eks-app-deployment) project.

1. **Build and push the image to Amazon ECR:**
   ```bash
   aws ecr create-repository --repository-name task-manager-api --region ap-southeast-2

   docker build -t task-manager-api .
   docker tag task-manager-api:latest <account-id>.dkr.ecr.ap-southeast-2.amazonaws.com/task-manager-api:latest

   aws ecr get-login-password --region ap-southeast-2 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-southeast-2.amazonaws.com

   docker push <account-id>.dkr.ecr.ap-southeast-2.amazonaws.com/task-manager-api:latest
   ```

2. **Set up the database** — recommended: Amazon RDS for PostgreSQL (simpler and more realistic than running stateful Postgres pods on Fargate).

3. **Create the Fargate profile for this namespace:**
   ```bash
   eksctl create fargateprofile --cluster democluster --region ap-southeast-2 \
     --name task-api-profile --namespace task-api
   ```

4. **Create the DB secret:**
   ```bash
   kubectl create secret generic task-db-secret -n task-api \
     --from-literal=DB_HOST=<your-rds-endpoint> \
     --from-literal=DB_PORT=5432 \
     --from-literal=DB_NAME=taskdb \
     --from-literal=DB_USER=taskuser \
     --from-literal=DB_PASSWORD=<your-password>
   ```

5. **Apply the manifests:**
   ```bash
   kubectl apply -f k8s/00-namespace.yaml
   kubectl apply -f k8s/02-deployment.yaml   # update image URI first
   kubectl apply -f k8s/03-service.yaml
   kubectl apply -f k8s/04-ingress.yaml
   ```

6. **Get the public ALB URL:**
   ```bash
   kubectl get ingress -n task-api
   ```

## Project Structure
```
task-manager-api/
├── src/main/java/com/kashmira/taskmanager/
│   ├── controller/     # REST endpoints
│   ├── service/        # Business logic
│   ├── repository/     # Data access (Spring Data JPA)
│   ├── model/           # JPA entities
│   ├── dto/             # Request/response objects
│   └── exception/       # Centralized error handling
├── k8s/                 # Kubernetes manifests (Deployment, Service, Ingress, Secret)
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## What I learned
- Structuring a Spring Boot app with proper layering (Controller/Service/Repository)
- Multi-stage Docker builds to keep runtime images small
- Environment-variable-driven configuration so the same image works locally, in Docker, and in Kubernetes
- Deploying a self-built application (not just a sample) to EKS behind a real ALB

## Next Steps
- Add authentication (Spring Security + JWT)
- Add pagination and filtering to `GET /api/tasks`
- Wire up CI/CD (GitHub Actions → ECR → EKS)
- Instrument with Prometheus + Grafana (see companion monitoring project)
