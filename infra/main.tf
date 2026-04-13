terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Uncomment to use S3 as remote state backend (recommended for teams):
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "crm-small/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "crm-small"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
