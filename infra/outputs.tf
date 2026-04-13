output "alb_url" {
  description = "Public URL of the Application Load Balancer"
  value       = "http://${aws_lb.main.dns_name}"
}

output "ecr_repository_url" {
  description = "ECR repository URL — use this in docker push and task definitions"
  value       = aws_ecr_repository.app.repository_url
}

output "rds_endpoint" {
  description = "RDS instance hostname (private, reachable only from ECS)"
  value       = aws_db_instance.postgres.address
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.app.name
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group for application logs"
  value       = aws_cloudwatch_log_group.app.name
}
