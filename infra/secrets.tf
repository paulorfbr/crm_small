resource "random_password" "db" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}?"
}

resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.app_name}/db-password"
  description             = "RDS master password for ${var.app_name}"
  recovery_window_in_days = 7

  tags = { Name = "${var.app_name}-db-password" }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db.result
}
