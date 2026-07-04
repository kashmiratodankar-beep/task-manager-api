# Monitoring & Observability

This adds Prometheus + Grafana monitoring on top of the Task Manager API, 
tested locally on Minikube (transferable to Amazon EKS with no code changes).

## Architecture
```
Spring Boot App (Actuator + Micrometer)
        |
        |  exposes /actuator/prometheus
        v
Prometheus (kube-prometheus-stack, scrapes every 15s via ServiceMonitor)
        |
        v
Grafana (custom dashboard: heap memory, request rate, response time)
```

## What's included
- `06-servicemonitor.yaml` — tells Prometheus to auto-discover and scrape 
  this app's metrics endpoint
- `grafana-dashboard.json` — exported custom Grafana dashboard with 3 panels:
  - **Heap Memory Usage** — `jvm_memory_used_bytes{area="heap"}`
  - **HTTP Request Rate** — `rate(http_server_requests_seconds_count[1m])`
  - **Average Response Time** — request duration sum / count, rated over 1m

## How it was set up
1. Installed `kube-prometheus-stack` via Helm (bundles Prometheus, Grafana, Alertmanager):
   ```bash
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
   ```
2. Deployed the Task Manager API into the cluster with a named Service port 
   (`http`) so Prometheus could target it by name.
3. Applied `06-servicemonitor.yaml` to register the scrape target.
4. Verified the target was `UP` on Prometheus's `/targets` page.
5. Built a custom Grafana dashboard from scratch (rather than using a generic 
   community template) with PromQL queries tailored to this app's actual metrics.
6. Generated real traffic against `/api/tasks` to validate the request-rate 
   and response-time panels with live data.

## Running it yourself (local / Minikube)
```bash
# 1. Start Minikube
minikube start

# 2. Install the monitoring stack
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring --create-namespace

# 3. Build and load the app image into Minikube
docker build -t task-manager-api:latest .
minikube image load task-manager-api:latest

# 4. Deploy the app (see k8s/05-local-deployment.yaml)
kubectl apply -f k8s/05-local-deployment.yaml

# 5. Register the ServiceMonitor
kubectl apply -f k8s/06-servicemonitor.yaml

# 6. Access Grafana
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
# then import grafana-dashboard.json via Dashboards -> New -> Import
```

## What I learned
- How Prometheus Operator's `ServiceMonitor` CRD connects a Kubernetes 
  Service to Prometheus's scrape config, without manually editing Prometheus's config file
- Why Service port **names** (not just numbers) matter for ServiceMonitor targeting
- How to debug a "target not found" issue by checking, in order: whether the 
  ServiceMonitor exists, whether the Service has matching labels, and whether 
  Prometheus's namespace selector actually watches that namespace
- PromQL basics: instant vector selection, `rate()` over a time window, and 
  combining two rated metrics to compute an average (sum/count pattern)
- Why community dashboard templates can silently break across library 
  versions, and how building a small custom dashboard from real metric 
  names is often more reliable than importing one

## Next Steps
- Add Alertmanager rules (e.g., alert if error rate > 5% or pod restarts repeatedly)
- Deploy this same setup to the EKS cluster (swap the local Deployment 
  manifest for `k8s/02-deployment.yaml`, which uses Fargate + ECR image)
- Add a Grafana panel for JVM garbage collection pause time
