output "lambda_function_arn" {
  description = "ARN da função AWS Lambda Auth"
  value       = aws_lambda_function.auth_lambda.arn
}

output "lambda_function_name" {
  description = "Nome da função AWS Lambda Auth"
  value       = aws_lambda_function.auth_lambda.function_name
}

output "lambda_invoke_arn" {
  description = "ARN de invocação do API Gateway para a função Lambda"
  value       = aws_lambda_function.auth_lambda.invoke_arn
}

output "lambda_role_arn" {
  description = "ARN da IAM Role utilizada pela Lambda"
  value       = aws_iam_role.lambda_exec.arn
}

output "lambda_security_group_id" {
  description = "ID do Security Group criado para a Lambda (se em VPC)"
  value       = try(aws_security_group.lambda_sg[0].id, null)
}
