# Distributed Lovable Clone — Backend

A distributed, microservices-based backend for an AI-powered application development platform inspired by Lovable. Built with Java 21 and Spring Boot 4.x, the backend consists of multiple services responsible for application functionality, API routing, configuration, service discovery, and workspace management.

The services are containerized with Docker and deployed on Google Kubernetes Engine (GKE) using Kubernetes.

## Overview

The Distributed Lovable Clone provides the backend foundation for an AI-powered application development platform. It separates application responsibilities into individual services, enabling modular development, independent service deployments, and integration with the frontend through HTTP APIs.

The frontend and backend are maintained in separate repositories but run as separate Kubernetes Deployments within the same GKE cluster.

## Tech Stack

- **Java 21** — Backend programming language
- **Spring Boot 4.x** — Backend application framework
- **Spring Cloud Gateway** — API gateway
- **Spring Cloud** — Microservices infrastructure
- **Maven** — Dependency management and build automation
- **Docker** — Containerization
- **Kubernetes** — Container orchestration
- **Google Kubernetes Engine (GKE)** — Cloud deployment platform
- **GitHub Actions** — CI/CD workflow automation

## Architecture

```text
                    User / Browser
                          |
                          v
                  React Frontend
                          |
                          | HTTP API Requests
                          v
                     API Gateway
                          |
          +---------------+----------------+
          |               |                |
          v               v                v
   Account Service  Workspace Service  Intelligence Service
          |               |                |
          +---------------+----------------+
                          |
                 Supporting Services
                    /           \
                   v             v
           Config Service   Discovery Service

             Common Library
          Shared Backend Code

       Frontend and Backend Workloads
             Google Kubernetes
               Engine (GKE)
```

The diagram illustrates the main repository modules conceptually. Actual service-to-service communication and runtime dependencies are determined by the application configuration.

## Microservices

| Service / Module | Responsibility |
|---|---|
| `api-gateway` | Entry point for routing requests to backend services |
| `account-service` | Account-related functionality |
| `workspace-service` | Workspace and project-related functionality |
| `intelligence-service` | AI and intelligence-related functionality |
| `config-service` | Application configuration management |
| `discovery-service` | Service discovery |
| `common-lib` | Shared code and utilities used by backend services |

## Repository Structure

```text
Distributed-Lovable-Clone/
├── .github/
│   └── workflows/
├── account-service/
├── api-gateway/
├── common-lib/
├── config-service/
├── discovery-service/
├── intelligence-service/
├── k8s/
├── workspace-service/
└── .gitignore
```

- `.github/workflows/` — GitHub Actions workflow definitions
- `account-service/` — Account service
- `api-gateway/` — API gateway
- `common-lib/` — Shared backend library
- `config-service/` — Configuration service
- `discovery-service/` — Service discovery
- `intelligence-service/` — Intelligence service
- `k8s/` — Kubernetes deployment and configuration manifests
- `workspace-service/` — Workspace service

## Frontend Integration

The frontend is built with React and TypeScript and is maintained in a separate repository.

The frontend communicates with backend APIs over HTTP through the configured API endpoint. Both frontend and backend workloads run within the same GKE cluster, using separate Kubernetes Deployments and the networking configuration defined for the application.

## Containerization and Deployment

Docker is used to package backend services into container images. Kubernetes manages the deployment and operation of these containers on GKE.

The repository includes Kubernetes manifests under the `k8s/` directory.

The deployment workflow includes the following general steps:

1. Build the backend services using Maven.
2. Build and publish Docker images.
3. Configure the required application settings and service endpoints.
4. Deploy the services using Kubernetes manifests.
5. Verify deployment, pod, and service status.

Useful Kubernetes commands:

```bash
kubectl get deployments -A
kubectl get pods -A
kubectl get services -A
```

These commands display resources across namespaces in the currently configured Kubernetes cluster.

## CI/CD

GitHub Actions workflows are maintained under `.github/workflows/`.

These workflows automate the tasks defined in their respective configuration files. Refer to the workflow definitions for the exact build, validation, image-publishing, and deployment steps.

## Local Development

### Prerequisites

- Java 21
- Maven or the Maven Wrapper included with the service
- Git
- Docker, when running containerized services
- Access to any required databases, infrastructure, and application configuration

### Clone the repository

```bash
git clone https://github.com/vaibhavshahi10/Distributed-Lovable-Clone.git
cd Distributed-Lovable-Clone
```

### Build the shared library

Build and install the shared library before building services that depend on it:

```bash
cd common-lib
./mvnw clean install
```

### Build a backend service

For example, to build the workspace service:

```bash
cd ../workspace-service
./mvnw clean package
```

Repeat the build for other services as required.

On Windows, run the Maven Wrapper using the appropriate command for your terminal and module layout.

### Run a service

From an individual service directory, use:

```bash
./mvnw spring-boot:run
```

Before starting a service, configure its required environment variables, ports, dependencies, and application properties.

## Future Enhancements

Potential improvements include enhanced observability, centralized logging, metrics, resilience mechanisms, and expanded automated testing, depending on project requirements.

## Author

**Vaibhav Shahi**

[GitHub Profile](https://github.com/vaibhavshahi10)
