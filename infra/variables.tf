variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (e.g. staging, production)"
  type        = string
  default     = "production"
}

variable "app_name" {
  description = "Application name, used as a prefix for all resource names"
  type        = string
  default     = "crm-small"
}

# ── Networking ──────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of AZs to deploy into (minimum 2 required for RDS)"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

# ── ECS / Application ───────────────────────────────────────────────────────────

variable "app_port" {
  description = "Port the Spring Boot application listens on"
  type        = number
  default     = 8080
}

variable "ecs_task_cpu" {
  description = "Fargate task CPU units (256 / 512 / 1024 / 2048 / 4096)"
  type        = number
  default     = 512
}

variable "ecs_task_memory" {
  description = "Fargate task memory in MiB"
  type        = number
  default     = 1024
}

variable "ecs_desired_count" {
  description = "Number of running ECS task instances"
  type        = number
  default     = 1
}

variable "ecr_image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

# ── RDS ─────────────────────────────────────────────────────────────────────────

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "crm_small"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "crmadmin"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "RDS storage in GiB"
  type        = number
  default     = 20
}
