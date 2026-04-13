#!/usr/bin/env bash
# build-and-push.sh — Build the Docker image and push it to ECR.
#
# Usage:
#   ./infra/scripts/build-and-push.sh [IMAGE_TAG]
#
# Prerequisites:
#   - AWS CLI configured (aws configure or IAM role)
#   - Docker daemon running
#   - Terraform already applied (ECR repository must exist)
#   - jq installed
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
INFRA_DIR="$PROJECT_ROOT/infra"

IMAGE_TAG="${1:-$(git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo "latest")}"

echo "==> Reading Terraform outputs..."
ECR_URL=$(terraform -chdir="$INFRA_DIR" output -raw ecr_repository_url)
AWS_REGION=$(terraform -chdir="$INFRA_DIR" output -raw alb_url | cut -d. -f3 2>/dev/null || echo "us-east-1")

# Derive the registry hostname from the ECR URL (format: <account>.dkr.ecr.<region>.amazonaws.com/<repo>)
ECR_REGISTRY=$(echo "$ECR_URL" | cut -d/ -f1)

echo "==> Logging in to ECR ($ECR_REGISTRY)..."
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

echo "==> Building Docker image (tag: $IMAGE_TAG)..."
docker build \
  --platform linux/amd64 \
  -t "${ECR_URL}:${IMAGE_TAG}" \
  -t "${ECR_URL}:latest" \
  "$PROJECT_ROOT"

echo "==> Pushing image..."
docker push "${ECR_URL}:${IMAGE_TAG}"
docker push "${ECR_URL}:latest"

echo ""
echo "Done. Image pushed:"
echo "  ${ECR_URL}:${IMAGE_TAG}"
echo "  ${ECR_URL}:latest"
echo ""
echo "To deploy, run:"
echo "  ./infra/scripts/deploy.sh $IMAGE_TAG"
