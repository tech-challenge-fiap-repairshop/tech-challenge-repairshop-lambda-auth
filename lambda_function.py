import os
import json
import urllib.request
import urllib.error

# URL da aplicação principal (configurável via variável de ambiente da AWS Lambda)
REPAIRSHOP_APP_URL = os.environ.get("REPAIRSHOP_APP_URL", "http://localhost:8080").rstrip('/')


class AuthClient:
    """Cliente responsável por realizar a chamada HTTP de login para a aplicação principal."""

    def __init__(self, base_url: str = REPAIRSHOP_APP_URL):
        self.login_url = f"{base_url}/auth/login"

    def login(self, identifier: str, password: str) -> dict:
        # Envia o identificador (CPF ou Email) no campo email/login esperado pela aplicação backend
        payload = json.dumps({"email": identifier, "password": password}).encode('utf-8')
        req = urllib.request.Request(
            self.login_url,
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as response:
                body = response.read().decode('utf-8')
                return json.loads(body)
        except urllib.error.HTTPError as e:
            error_body = e.read().decode('utf-8')
            raise ValueError(f"Auth failed [{e.code}]: {error_body}")
        except urllib.error.URLError as e:
            raise ConnectionError(f"Could not connect to RepairShop App: {e.reason}")


def lambda_handler(event, context):
    """
    Handler oficial da AWS Lambda para API Gateway.
    Recebe a requisição da API Gateway (CPF/Email e Senha), 
    executa o login na aplicação principal e retorna o token JWT no padrão Proxy Integration.
    """
    try:
        # Trata payload vindo do API Gateway (proxy event) ou invocação direta
        body = event.get("body") if isinstance(event, dict) and "body" in event else event
        if isinstance(body, str):
            body = json.loads(body) if body else {}
        if not isinstance(body, dict):
            body = {}

        # Suporta tanto 'cpf' quanto 'email' no payload
        identifier = body.get("cpf") or body.get("email")
        password = body.get("password")

        if not identifier or not password:
            return {
                "statusCode": 400,
                "headers": {
                    "Content-Type": "application/json",
                    "Access-Control-Allow-Origin": "*"
                },
                "body": json.dumps({"error": "Os campos 'cpf' (ou 'email') e 'password' são obrigatórios."})
            }

        client = AuthClient()
        token_response = client.login(identifier, password)

        return {
            "statusCode": 200,
            "headers": {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": "*"
            },
            "body": json.dumps(token_response)
        }

    except ValueError as e:
        return {
            "statusCode": 401,
            "headers": {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": "*"
            },
            "body": json.dumps({"error": str(e)})
        }
    except Exception as e:
        return {
            "statusCode": 500,
            "headers": {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": "*"
            },
            "body": json.dumps({"error": f"Erro interno na Lambda: {str(e)}"})
        }

