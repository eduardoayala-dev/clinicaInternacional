import argparse
import base64
import hashlib
import http.server
import json
import os
from pathlib import Path
import secrets
import time
import urllib.error
import urllib.parse
import urllib.request
import webbrowser


def autorizar(cliente_json, remitente, proyecto):
    if not remitente.endswith("@gmail.com") or any(c.isspace() for c in remitente):
        raise ValueError("Usa la dirección Gmail dedicada al proyecto.")
    cliente = json.loads(cliente_json.read_text(encoding="utf-8"))["installed"]
    estado = secrets.token_urlsafe(32)
    verificador = secrets.token_urlsafe(64)
    desafio = base64.urlsafe_b64encode(hashlib.sha256(verificador.encode()).digest()).decode().rstrip("=")
    resultado = {}

    class Callback(http.server.BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass

        def do_GET(self):
            url = urllib.parse.urlsplit(self.path)
            parametros = urllib.parse.parse_qs(url.query)
            if url.path != "/" or not secrets.compare_digest(parametros.get("state", [""])[0], estado):
                self.send_error(400, "Solicitud no valida")
                return
            if "error" in parametros:
                resultado["error"] = True
            elif parametros.get("code"):
                resultado["code"] = parametros["code"][0]
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Referrer-Policy", "no-referrer")
            self.end_headers()
            self.wfile.write("Puedes cerrar esta ventana y volver a la terminal.".encode())

    with http.server.HTTPServer(("127.0.0.1", 0), Callback) as servidor:
        servidor.timeout = 1
        redireccion = f"http://127.0.0.1:{servidor.server_port}/"
        parametros = dict(client_id=cliente["client_id"], redirect_uri=redireccion,
                          response_type="code", scope="https://www.googleapis.com/auth/gmail.send",
                          access_type="offline", prompt="consent", state=estado,
                          code_challenge=desafio, code_challenge_method="S256", login_hint=remitente)
        enlace = "https://accounts.google.com/o/oauth2/v2/auth?" + urllib.parse.urlencode(parametros)
        print("Se abrirá Google. Autoriza solamente la cuenta Gmail del proyecto.")
        print("Si no se abre el navegador, vuelve a ejecutar desde una terminal de Windows.")
        webbrowser.open(enlace)
        limite = time.monotonic() + 300
        while not resultado and time.monotonic() < limite:
            servidor.handle_request()
    if "code" not in resultado:
        raise ValueError("La autorización fue cancelada o agotó el tiempo. No se cambió la configuración.")
    cuerpo = urllib.parse.urlencode(dict(client_id=cliente["client_id"], client_secret=cliente["client_secret"],
                                        code=resultado["code"], redirect_uri=redireccion,
                                        grant_type="authorization_code", code_verifier=verificador)).encode()
    solicitud = urllib.request.Request("https://oauth2.googleapis.com/token", data=cuerpo,
                                      headers={"Content-Type": "application/x-www-form-urlencoded"})
    with urllib.request.urlopen(solicitud, timeout=15) as respuesta:
        datos = json.loads(respuesta.read(65_537))
    if "refresh_token" not in datos or "https://www.googleapis.com/auth/gmail.send" not in datos.get("scope", "").split():
        raise ValueError("Google no concedió el permiso de envío y renovación requerido.")
    valores = dict(MAIL_MODE="disabled", MAIL_FROM=remitente, GMAIL_CLIENT_ID=cliente["client_id"],
                   GMAIL_CLIENT_SECRET=cliente["client_secret"], GMAIL_REFRESH_TOKEN=datos["refresh_token"])
    if any("\r" in valor or "\n" in valor for valor in valores.values()):
        raise ValueError("Configuración de Google inválida.")
    destino = proyecto / ".env.mail"
    temporal = proyecto / ".env.mail.tmp"
    os.umask(0o077)
    try:
        with temporal.open("x", encoding="utf-8", newline="\n") as archivo:
            archivo.write("".join(f"{clave}={valor}\n" for clave, valor in valores.items()))
        temporal.replace(destino)
    finally:
        temporal.unlink(missing_ok=True)
    print("Autorización guardada en .env.mail sin mostrar claves ni tokens.")
    print("No se envió ningún correo. MAIL_MODE sigue disabled.")
    print("Comprueba que MAIL_FROM sea la cuenta que autorizaste antes de activar el envío.")


def main():
    proyecto = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description="Autorizar Gmail API con OAuth2 local y PKCE.")
    parser.add_argument("--cliente", type=Path, default=proyecto / ".env.gmail-client.json")
    parser.add_argument("--remitente", required=True)
    argumentos = parser.parse_args()
    try:
        autorizar(argumentos.cliente, argumentos.remitente, proyecto)
    except (ValueError, KeyError, OSError, urllib.error.URLError):
        parser.exit(1, "No se completó la autorización. Revisa el cliente de escritorio, Gmail y el permiso gmail.send. No compartas claves.\n")


if __name__ == "__main__":
    main()
