import base64
import os
import sys
import uuid

import requests


# ============================================================
# CONFIGURAÇÃO
# ============================================================

API = os.getenv(
    "STAGING_API_URL",
    "https://saltos-nas-palhacadas-api-staging.onrender.com/api/v1",
).rstrip("/")

FRONTEND_ORIGIN = os.getenv(
    "STAGING_FRONTEND_ORIGIN",
    "https://dev.saltos-nas-palhacadas.pages.dev",
).rstrip("/")

ADMIN_EMAIL = os.getenv("STAGING_ADMIN_EMAIL")
ADMIN_PASSWORD = os.getenv("STAGING_ADMIN_PASSWORD")

CUSTOMER_A_EMAIL = os.getenv("STAGING_CUSTOMER_A_EMAIL")
CUSTOMER_A_PASSWORD = os.getenv("STAGING_CUSTOMER_A_PASSWORD")

CUSTOMER_B_EMAIL = os.getenv("STAGING_CUSTOMER_B_EMAIL")
CUSTOMER_B_PASSWORD = os.getenv("STAGING_CUSTOMER_B_PASSWORD")

TIMEOUT = 30

passed = 0
failed = 0
warnings = 0


# ============================================================
# OUTPUT DOS TESTES
# ============================================================

def pass_test(name, detail=""):
    global passed

    passed += 1

    suffix = f" ({detail})" if detail else ""

    print(f"[PASS] {name}{suffix}")


def fail_test(name, detail=""):
    global failed

    failed += 1

    suffix = f" ({detail})" if detail else ""

    print(f"[FAIL] {name}{suffix}")


def warn(name, detail=""):
    global warnings

    warnings += 1

    suffix = f" ({detail})" if detail else ""

    print(f"[WARN] {name}{suffix}")


def expect_status(name, response, expected):
    if response.status_code == expected:
        pass_test(name, str(response.status_code))
        return True

    body = response.text[:300].replace("\n", " ")

    fail_test(
        name,
        (
            f"esperado={expected}, "
            f"recebido={response.status_code}, "
            f"body={body!r}"
        ),
    )

    return False


# ============================================================
# HELPERS
# ============================================================

def require_env(name, value):
    if not value:
        print(f"[FATAL] Variável {name} não definida.")
        sys.exit(2)


def auth_headers(token):
    return {
        "Authorization": f"Bearer {token}",
    }


def login(email, password, expected_role, label):
    response = requests.post(
        f"{API}/auth/login",
        json={
            "email": email,
            "password": password,
        },
        timeout=TIMEOUT,
    )

    if not expect_status(
        f"Login {label}",
        response,
        200,
    ):
        return None

    try:
        data = response.json()

    except ValueError:
        fail_test(
            f"Resposta JSON {label}",
            "resposta não é JSON",
        )
        return None

    # --------------------------------------------------------
    # Validar role
    # --------------------------------------------------------

    role = data.get("role")

    if role != expected_role:
        fail_test(
            f"Role {label}",
            f"esperado={expected_role}, recebido={role}",
        )
        return None

    pass_test(
        f"Role {label}",
        expected_role,
    )

    # --------------------------------------------------------
    # IMPORTANTE:
    # Backend devolve "accessToken", não "token".
    # --------------------------------------------------------

    token = data.get("accessToken")

    if not isinstance(token, str) or not token.strip():
        fail_test(
            f"Token {label}",
            "accessToken ausente",
        )
        return None

    pass_test(
        f"Token {label}",
        "recebido",
    )

    # Não imprimir o JWT.
    return token


# ============================================================
# RESUMO
# ============================================================

def summary():
    print()
    print("=" * 70)
    print(" RESULTADO")
    print("=" * 70)

    print(f"PASS: {passed}")
    print(f"FAIL: {failed}")
    print(f"WARN: {warnings}")

    print()

    if failed == 0:
        print("STAGING RESULT: PASS")
    else:
        print("STAGING RESULT: FAIL")


# ============================================================
# TESTES
# ============================================================

def main():
    print("=" * 70)
    print(" SALTOS NAS PALHAÇADAS - STAGING SMOKE / SECURITY TESTS")
    print("=" * 70)

    print()
    print(f"API: {API}")
    print(f"Frontend origin: {FRONTEND_ORIGIN}")
    print()

    # ========================================================
    # SEGURANÇA CONTRA EXECUÇÃO EM PRODUÇÃO
    # ========================================================

    if "staging" not in API.lower():
        print("[FATAL] A URL não parece ser de staging.")
        print("[FATAL] O script recusou executar para proteger produção.")
        sys.exit(2)

    # ========================================================
    # VALIDAR VARIÁVEIS
    # ========================================================

    require_env(
        "STAGING_ADMIN_EMAIL",
        ADMIN_EMAIL,
    )

    require_env(
        "STAGING_ADMIN_PASSWORD",
        ADMIN_PASSWORD,
    )

    require_env(
        "STAGING_CUSTOMER_A_EMAIL",
        CUSTOMER_A_EMAIL,
    )

    require_env(
        "STAGING_CUSTOMER_A_PASSWORD",
        CUSTOMER_A_PASSWORD,
    )

    require_env(
        "STAGING_CUSTOMER_B_EMAIL",
        CUSTOMER_B_EMAIL,
    )

    require_env(
        "STAGING_CUSTOMER_B_PASSWORD",
        CUSTOMER_B_PASSWORD,
    )

    # Extrair:
    #
    # https://...onrender.com
    #
    # a partir de:
    #
    # https://...onrender.com/api/v1

    origin = API.split("/api/v1")[0]

    # ========================================================
    # 1. HEALTH
    # ========================================================

    print()
    print("--- HEALTH ---")

    response = requests.get(
        f"{origin}/actuator/health",
        timeout=TIMEOUT,
    )

    if expect_status(
        "Health endpoint",
        response,
        200,
    ):
        try:
            health = response.json()

            status = health.get("status")

            if status == "UP":
                pass_test(
                    "Backend status",
                    "UP",
                )

            else:
                fail_test(
                    "Backend status",
                    f"recebido={status}",
                )

        except ValueError:
            fail_test(
                "Health JSON",
                "resposta não é JSON",
            )

    # ========================================================
    # 2. LOGIN
    # ========================================================

    print()
    print("--- AUTHENTICATION ---")

    admin_token = login(
        ADMIN_EMAIL,
        ADMIN_PASSWORD,
        "ADMIN",
        "ADMIN",
    )

    customer_a_token = login(
        CUSTOMER_A_EMAIL,
        CUSTOMER_A_PASSWORD,
        "CUSTOMER",
        "Cliente A",
    )

    customer_b_token = login(
        CUSTOMER_B_EMAIL,
        CUSTOMER_B_PASSWORD,
        "CUSTOMER",
        "Cliente B",
    )

    if not all([
        admin_token,
        customer_a_token,
        customer_b_token,
    ]):
        print()
        print(
            "[FATAL] Não foi possível obter "
            "as três sessões."
        )

        summary()

        sys.exit(1)

    # ========================================================
    # 3. PASSWORD ERRADA
    # ========================================================

    print()
    print("--- INVALID LOGIN ---")

    wrong_password = requests.post(
        f"{API}/auth/login",
        json={
            "email": CUSTOMER_A_EMAIL,
            "password": (
                "esta-password-esta-errada-123456"
            ),
        },
        timeout=TIMEOUT,
    )

    expect_status(
        "Password errada é rejeitada",
        wrong_password,
        401,
    )

    # ========================================================
    # 4. AUTH / ME
    # ========================================================

    print()
    print("--- AUTH / ME ---")

    sessions = [
        (
            "ADMIN",
            admin_token,
            "ADMIN",
        ),
        (
            "Cliente A",
            customer_a_token,
            "CUSTOMER",
        ),
        (
            "Cliente B",
            customer_b_token,
            "CUSTOMER",
        ),
    ]

    for label, token, expected_role in sessions:

        response = requests.get(
            f"{API}/auth/me",
            headers=auth_headers(token),
            timeout=TIMEOUT,
        )

        if expect_status(
            f"/auth/me - {label}",
            response,
            200,
        ):
            try:
                user = response.json()

                role = user.get("role")

                if role == expected_role:
                    pass_test(
                        f"/auth/me role - {label}",
                        role,
                    )

                else:
                    fail_test(
                        f"/auth/me role - {label}",
                        (
                            f"esperado={expected_role}, "
                            f"recebido={role}"
                        ),
                    )

            except ValueError:
                fail_test(
                    f"/auth/me JSON - {label}",
                    "resposta não é JSON",
                )

    # ========================================================
    # 5. CRIAR PERFIL TEMPORÁRIO
    # ========================================================

    print()
    print("--- ADMIN AUTHORIZATION ---")

    test_slug = (
        f"smoke-test-{uuid.uuid4().hex[:10]}"
    )

    profile_body = {
        "slug": test_slug,
        "name": "Smoke Test Staging",
        "role": "DJ",
        "description": (
            "Perfil temporário criado automaticamente "
            "pelo smoke test."
        ),
    }

    # --------------------------------------------------------
    # Anónimo → ADMIN
    # --------------------------------------------------------

    anonymous_admin = requests.post(
        f"{API}/admin/profiles",
        json=profile_body,
        timeout=TIMEOUT,
    )

    expect_status(
        "Anónimo não pode criar perfil ADMIN",
        anonymous_admin,
        403,
    )

    # --------------------------------------------------------
    # CUSTOMER → ADMIN
    # --------------------------------------------------------

    customer_admin = requests.post(
        f"{API}/admin/profiles",
        headers=auth_headers(
            customer_a_token
        ),
        json=profile_body,
        timeout=TIMEOUT,
    )

    expect_status(
        "CUSTOMER não pode criar perfil ADMIN",
        customer_admin,
        403,
    )

    # --------------------------------------------------------
    # ADMIN → ADMIN
    # --------------------------------------------------------

    admin_create = requests.post(
        f"{API}/admin/profiles",
        headers=auth_headers(
            admin_token
        ),
        json=profile_body,
        timeout=TIMEOUT,
    )

    profile_created = expect_status(
        "ADMIN pode criar perfil",
        admin_create,
        201,
    )

    # O endpoint POST /api/v1/admin/profiles devolve 201
    # e aceita estes quatro campos obrigatórios. Os restantes
    # são opcionais. Isto corresponde ao controller atual.
    # filecite não pertence ao código; comentário apenas aqui.

    # ========================================================
    # 6. PERFIL PÚBLICO
    # ========================================================

    print()
    print("--- PUBLIC PROFILE ---")

    if profile_created:

        public_profile = requests.get(
            f"{API}/profiles/{test_slug}",
            timeout=TIMEOUT,
        )

        if expect_status(
            "Perfil criado fica publicamente acessível",
            public_profile,
            200,
        ):
            try:
                profile_json = (
                    public_profile.json()
                )

                received_slug = (
                    profile_json.get("slug")
                )

                if received_slug == test_slug:
                    pass_test(
                        "Perfil público tem slug correto",
                        test_slug,
                    )

                else:
                    fail_test(
                        "Perfil público tem slug correto",
                        (
                            f"esperado={test_slug}, "
                            f"recebido={received_slug}"
                        ),
                    )

            except ValueError:
                fail_test(
                    "Perfil público JSON",
                    "resposta não é JSON",
                )

    else:
        warn(
            "Teste do perfil público ignorado",
            "perfil ADMIN não foi criado",
        )

    # ========================================================
    # 7. UPLOAD DE AVATAR
    # ========================================================

    print()
    print("--- PRIVATE MEDIA ---")

    # PNG 1x1 válido.
    #
    # Não dependemos de qualquer ficheiro local.

    png = base64.b64decode(
        (
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB"
            "CAQAAAC1HAwCAAAAC0lEQVR42mP8/x8A"
            "AusB9Y9Z2D8AAAAASUVORK5CYII="
        )
    )

    upload = requests.post(
        f"{API}/media",
        headers=auth_headers(
            customer_a_token
        ),
        files={
            "file": (
                "smoke-avatar.png",
                png,
                "image/png",
            ),
        },
        timeout=TIMEOUT,
    )

    private_url = None

    if expect_status(
        "Cliente A pode fazer upload privado",
        upload,
        201,
    ):

        try:
            upload_json = upload.json()

            private_url = (
                upload_json.get("url")
            )

            media_id = (
                upload_json.get("id")
            )

            content_type = (
                upload_json.get("contentType")
            )

            if media_id:
                pass_test(
                    "Upload devolve media id",
                    "presente",
                )

            else:
                fail_test(
                    "Upload devolve media id",
                    "id ausente",
                )

            if content_type == "image/png":
                pass_test(
                    "Content-Type do upload",
                    content_type,
                )

            else:
                fail_test(
                    "Content-Type do upload",
                    (
                        f"esperado=image/png, "
                        f"recebido={content_type}"
                    ),
                )

            if (
                isinstance(
                    private_url,
                    str,
                )
                and (
                    "/api/v1/private-media/"
                    in private_url
                )
            ):
                pass_test(
                    "Upload é armazenado como privado",
                    "/private-media/",
                )

            else:
                fail_test(
                    "Upload é armazenado como privado",
                    f"url={private_url!r}",
                )

        except ValueError:
            fail_test(
                "Resposta JSON do upload",
                "resposta não é JSON",
            )

    # ========================================================
    # 8. ISOLAMENTO DE MEDIA PRIVADA
    # ========================================================

    if private_url:

        # ----------------------------------------------------
        # Dono
        # ----------------------------------------------------

        owner = requests.get(
            private_url,
            headers=auth_headers(
                customer_a_token
            ),
            timeout=TIMEOUT,
        )

        expect_status(
            (
                "Cliente A vê o próprio "
                "ficheiro privado"
            ),
            owner,
            200,
        )

        # ----------------------------------------------------
        # Outro cliente
        # ----------------------------------------------------

        other_customer = requests.get(
            private_url,
            headers=auth_headers(
                customer_b_token
            ),
            timeout=TIMEOUT,
        )

        expect_status(
            (
                "Cliente B não vê ficheiro "
                "do Cliente A"
            ),
            other_customer,
            404,
        )

        # ----------------------------------------------------
        # Anónimo
        # ----------------------------------------------------

        anonymous = requests.get(
            private_url,
            timeout=TIMEOUT,
        )

        expect_status(
            "Anónimo não vê ficheiro privado",
            anonymous,
            403,
        )

        # ----------------------------------------------------
        # Admin
        #
        # O código atual permite owner ou ADMIN.
        # ----------------------------------------------------

        admin_media = requests.get(
            private_url,
            headers=auth_headers(
                admin_token
            ),
            timeout=TIMEOUT,
        )

        expect_status(
            "ADMIN pode consultar media privada",
            admin_media,
            200,
        )

        # ----------------------------------------------------
        # Tentar mesma filename no endpoint público
        # ----------------------------------------------------

        public_url = private_url.replace(
            "/api/v1/private-media/",
            "/api/v1/media/",
        )

        public_attempt = requests.get(
            public_url,
            timeout=TIMEOUT,
        )

        if public_attempt.status_code in (403, 404):
            pass_test(
                "Upload privado não está disponível publicamente",
                str(public_attempt.status_code),
            )
        else:
            fail_test(
                "Upload privado não está disponível publicamente",
                (
                    "esperado=403/404, "
                    f"recebido={public_attempt.status_code}, "
                    f"body={public_attempt.text[:300]!r}"
                ),
            )

    else:
        warn(
            "Testes de media privada ignorados",
            "upload não produziu URL válida",
        )

    # ========================================================
    # 9. SVG MALICIOSO
    # ========================================================

    print()
    print("--- INVALID UPLOADS ---")

    svg_payload = b"""
    <svg xmlns="http://www.w3.org/2000/svg">
        <script>alert('xss')</script>
    </svg>
    """

    svg_response = requests.post(
        f"{API}/media",
        headers=auth_headers(
            customer_a_token
        ),
        files={
            "file": (
                "smoke-script.svg",
                svg_payload,
                "image/svg+xml",
            ),
        },
        timeout=TIMEOUT,
    )

    expect_status(
        "SVG potencialmente perigoso é rejeitado",
        svg_response,
        400,
    )

    # ========================================================
    # 10. UPLOAD SEM AUTENTICAÇÃO
    # ========================================================

    anonymous_upload = requests.post(
        f"{API}/media",
        files={
            "file": (
                "anonymous.png",
                png,
                "image/png",
            ),
        },
        timeout=TIMEOUT,
    )

    expect_status(
        "Anónimo não pode fazer upload de avatar",
        anonymous_upload,
        403,
    )

    # ========================================================
    # 11. CORS
    # ========================================================

    print()
    print("--- CORS ---")

    cors_response = requests.options(
        f"{API}/profiles",
        headers={
            "Origin": FRONTEND_ORIGIN,
            (
                "Access-Control-Request-Method"
            ): "GET",
        },
        timeout=TIMEOUT,
    )

    if cors_response.status_code in (
        200,
        204,
    ):

        allowed_origin = (
            cors_response.headers.get(
                "Access-Control-Allow-Origin"
            )
        )

        if allowed_origin == FRONTEND_ORIGIN:

            pass_test(
                (
                    "CORS permite origin "
                    "de staging"
                ),
                allowed_origin,
            )

        else:
            fail_test(
                "CORS staging",
                (
                    "Access-Control-Allow-Origin="
                    f"{allowed_origin!r}"
                ),
            )

    else:
        fail_test(
            "CORS preflight",
            (
                f"status="
                f"{cors_response.status_code}"
            ),
        )

    # ========================================================
    # 12. ORIGIN NÃO AUTORIZADA
    # ========================================================

    evil_origin = (
        "https://evil-example.invalid"
    )

    evil_cors = requests.options(
        f"{API}/profiles",
        headers={
            "Origin": evil_origin,
            (
                "Access-Control-Request-Method"
            ): "GET",
        },
        timeout=TIMEOUT,
    )

    evil_allowed_origin = (
        evil_cors.headers.get(
            "Access-Control-Allow-Origin"
        )
    )

    if evil_allowed_origin != evil_origin:

        pass_test(
            "CORS não autoriza origin desconhecida",
            (
                f"status="
                f"{evil_cors.status_code}"
            ),
        )

    else:
        fail_test(
            "CORS não autoriza origin desconhecida",
            (
                "origin maliciosa apareceu em "
                "Access-Control-Allow-Origin"
            ),
        )

    # ========================================================
    # 13. CLEANUP
    # ========================================================

    print()
    print("--- CLEANUP ---")

    if profile_created:

        cleanup = requests.delete(
            f"{API}/admin/profiles/{test_slug}",
            headers=auth_headers(
                admin_token
            ),
            timeout=TIMEOUT,
        )

        if cleanup.status_code == 204:

            pass_test(
                "Cleanup perfil temporário",
                "204",
            )

            # Confirmar que desapareceu.

            after_cleanup = requests.get(
                f"{API}/profiles/{test_slug}",
                timeout=TIMEOUT,
            )

            expect_status(
                (
                    "Perfil deixa de existir "
                    "após cleanup"
                ),
                after_cleanup,
                404,
            )

        else:
            warn(
                "Cleanup perfil temporário",
                (
                    f"status="
                    f"{cleanup.status_code}, "
                    f"body={cleanup.text[:200]!r}"
                ),
            )

    else:
        warn(
            "Cleanup de perfil ignorado",
            "nenhum perfil foi criado",
        )

    # ========================================================
    # RESULTADO FINAL
    # ========================================================

    summary()

    if failed:
        sys.exit(1)


# ============================================================
# ENTRYPOINT
# ============================================================

if __name__ == "__main__":

    try:
        main()

    except requests.Timeout:
        print()
        print(
            "[FATAL] Timeout ao comunicar "
            "com o staging."
        )
        sys.exit(2)

    except requests.ConnectionError as exc:
        print()
        print(
            f"[FATAL] Erro de ligação: {exc}"
        )
        sys.exit(2)

    except requests.RequestException as exc:
        print()
        print(
            f"[FATAL] Erro HTTP/rede: {exc}"
        )
        sys.exit(2)

    except KeyboardInterrupt:
        print()
        print("Testes interrompidos.")
        sys.exit(130)