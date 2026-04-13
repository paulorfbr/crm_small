#!/usr/bin/env bash
# deploy.sh — Update the ECS service to run the latest image.
#
# Usage:
#   ./infra/scripts/deploy.sh [IMAGE_TAG]
#
# Workflow:
#   1. terraform apply  (idempotent — skips if nothing changed)
#   2. aws ecs update-service --force-new-deployment
#   3. Wait for service to become stable
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/.."

IMAGE_TAG="${1:-latest}"

echo "==> Applying Terraform..."
terraform -chdir="$INFRA_DIR" apply \
  -var "ecr_image_tag=$IMAGE_TAG" \
  -auto-approve

echo ""
echo "==> Reading deployment targets from Terraform outputs..."
CLUSTER=$(terraform -chdir="$INFRA_DIR" output -raw ecs_cluster_name)
SERVICE=$(terraform -chdir="$INFRA_DIR" output -raw ecs_service_name)
ALB_URL=$(terraform -chdir="$INFRA_DIR" output -raw alb_url)

echo "==> Forcing new ECS deployment (cluster=$CLUSTER, service=$SERVICE)..."
aws ecs update-service \
  --cluster "$CLUSTER" \
  --service "$SERVICE" \
  --force-new-deployment \
  --query "service.deployments[0].status" \
  --output text

echo "==> Waiting for service to stabilise (this may take 2–5 minutes)..."
aws ecs wait services-stable \
  --cluster "$CLUSTER" \
  --services "$SERVICE"

echo ""
echo "Deployment complete."
echo "Application URL: $ALB_URL"
echo ""
echo "Tail logs:"
echo "  aws logs tail /ecs/crm-small --follow"
