# crm-small

CRM for IT services — LTV and RFM analytics.

## Functionalities

### Companies
![crm_companies.png](marketing/crm_companies.png)

The company list view provides a filterable, sortable table of all clients. Each row shows the company name, status (Active, Churned, Lost), RFM segment (Champion, Loyal, At Risk, etc.), BCG quadrant (Star, Cash Cow, Question Mark, Dog), last invoice amount, last activity date, and quick actions. Filter buttons at the top allow narrowing by status or BCG quadrant at a glance.

### Company BCG, LTV
![crm_company_bcg.png](marketing/crm_company_bcg.png)

The company detail analytics tab consolidates key financial and behavioral metrics in one view. Summary cards show total LTV, number of active contracts, and the latest invoice amount. The RFM Analysis panel displays individual Recency, Frequency, and Monetary scores (each out of 5) alongside the derived segment (e.g. Champion). The BCG Position panel plots the company on the Growth-Share matrix and highlights the current quadrant. A Revenue History bar chart covering the last 12 months rounds out the view.

### RFM Analysis
![crm_rfm_matrix.png](marketing/crm_rfm_matrix.png)

The portfolio-level RFM Analysis page segments all companies using NTILE(5) scoring calculated nightly. Segment summary cards (Champions, Loyal, New Customers, At Risk, etc.) show client counts and total revenue per segment. The R×F Score Heatmap visualises the distribution of clients across all Recency/Frequency combinations. A Monetary Score Distribution bar chart breaks down revenue by monetary score band. A Segment Movements panel tracks quarter-over-quarter migrations between segments.

## Prerequisites

- Java 21
- Maven 3.x
- PostgreSQL 14+

## Database Setup

Create the database and user:

```sql
CREATE DATABASE crm_small;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE crm_small TO postgres;
```

The default connection expects PostgreSQL running on `localhost:5432` with:

| Property | Value     |
|----------|-----------|
| Host     | localhost |
| Port     | 5433      |
| Database | crm_small |
| Username | postgres  |
| Password | postgres  |

To use different credentials, edit `src/main/resources/application.properties`.

Schema migrations are applied automatically on startup via Flyway.

## Running the Application

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/crm-small-0.0.1-SNAPSHOT.jar
```

The API starts on `http://localhost:8080`.

## Running Tests

```bash
./mvnw test
```

Tests use Testcontainers — Docker must be running.

## UI

Static HTML pages are located in the `ui/` directory:

- `dashboard.html` — main dashboard
- `companies.html` — company list
- `company-detail.html` — company detail view
- `analytics-rfm.html` — RFM analytics

Open these files directly in a browser while the backend is running.

## Analytics

LTV and RFM scores are recalculated nightly at 02:00 AM (configurable via `crm.analytics.cron` in `application.properties`).

---

## Deploying to AWS

The `infra/` directory contains Terraform code that provisions the full AWS stack:

```
ALB (port 80, public) → ECS Fargate (port 8080, private) → RDS PostgreSQL 14 (private)
```

Resources created: VPC, public/private subnets, ALB, ECS Fargate cluster, RDS PostgreSQL 14, ECR repository, Secrets Manager (DB password), CloudWatch log group.

### Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| AWS CLI | v2 | https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html |
| Terraform | ≥ 1.5 | https://developer.hashicorp.com/terraform/install |
| Docker | any | https://docs.docker.com/get-docker/ |

Configure AWS credentials before running any command:

```bash
aws configure          # or use IAM role / AWS SSO
aws sts get-caller-identity   # verify access
```

### Step 1 — Provision infrastructure

```bash
cd infra
terraform init
terraform plan         # review what will be created
terraform apply        # type 'yes' to confirm (~5 minutes)
```

After apply, Terraform prints the key outputs:

```
alb_url              = "http://crm-small-alb-xxxx.us-east-1.elb.amazonaws.com"
ecr_repository_url   = "123456789.dkr.ecr.us-east-1.amazonaws.com/crm-small"
rds_endpoint         = "crm-small-db.xxxx.us-east-1.rds.amazonaws.com"
cloudwatch_log_group = "/ecs/crm-small"
```

### Step 2 — Build and push the Docker image

```bash
chmod +x infra/scripts/build-and-push.sh
./infra/scripts/build-and-push.sh          # uses git SHA as tag automatically
```

The script logs in to ECR, builds a linux/amd64 image, and pushes it. On Apple Silicon (M-series) the `--platform linux/amd64` flag is applied automatically.

### Step 3 — Deploy to ECS

```bash
chmod +x infra/scripts/deploy.sh
./infra/scripts/deploy.sh                  # forces a new ECS deployment and waits for stable
```

The script runs `terraform apply` (idempotent), triggers a rolling deployment, and waits until the service is stable. The application URL is printed on completion.

### Updating the application

For every new release, repeat steps 2 and 3:

```bash
./infra/scripts/build-and-push.sh
./infra/scripts/deploy.sh
```

ECS performs a rolling update with zero downtime — the old task stays running until the new one passes the ALB health check (`GET /actuator/health → 200`).

### Configuration overrides

Create `infra/terraform.tfvars` (gitignored — never commit this file) to override defaults:

```hcl
# infra/terraform.tfvars
aws_region        = "us-east-1"
environment       = "production"
db_instance_class = "db.t3.small"   # scale up as needed
ecs_desired_count = 2               # run 2 tasks for redundancy
ecs_task_cpu      = 1024
ecs_task_memory   = 2048
```

### Viewing logs

```bash
# Stream live application logs
aws logs tail /ecs/crm-small --follow

# Filter for errors only
aws logs tail /ecs/crm-small --follow --filter-pattern "ERROR"

# View recent RFM pipeline runs
aws logs tail /ecs/crm-small --follow --filter-pattern "analytics"
```

### Teardown

```bash
cd infra
terraform destroy      # type 'yes' — this deletes all AWS resources including the database
```

> **Note:** RDS has `deletion_protection = true` and will take a final snapshot before deletion. Disable it first if you want a clean destroy:
> ```bash
> terraform apply -var="db_instance_class=db.t3.micro" -target=aws_db_instance.postgres
> # or manually disable deletion protection in the AWS Console first
> ```

---

**Built with ❤️ by the PRF IT Solutions Team**

*This platform is designed to help small PMEs through comprehensive business intelligence for marketing strategy and monitoring.*