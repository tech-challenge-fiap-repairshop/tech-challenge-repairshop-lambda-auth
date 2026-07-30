import sys
import json
from lambda_function import lambda_handler

if __name__ == "__main__":
    print("--- Executando Lambda Auth (API Gateway Simulator) ---")
    
    # Se passar argumentos via linha de comando: python main.py <cpf_ou_email> <password>
    if len(sys.argv) >= 3:
        cpf_or_email = sys.argv[1]
        password = sys.argv[2]
    else:
        cpf_or_email = input("Digite o CPF (ou E-mail): ").strip()
        password = input("Digite a Senha: ").strip()

    # Simula um evento de Proxy Integration do AWS API Gateway
    event = {
        "resource": "/auth/login",
        "path": "/auth/login",
        "httpMethod": "POST",
        "headers": {
            "Content-Type": "application/json"
        },
        "body": json.dumps({
            "cpf": cpf_or_email,
            "password": password
        })
    }

    response = lambda_handler(event, None)
    
    print("\n[Resposta da AWS Lambda / API Gateway]:")
    print(f"Status Code: {response['statusCode']}")
    print(f"Headers: {response['headers']}")
    print(f"Body: {response['body']}")

