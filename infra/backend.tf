terraform {
  backend "s3" {
    bucket  = "fiap-repairshop2"
    key     = "terraform-config/lambda-auth-tfstate/terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
  }
}
