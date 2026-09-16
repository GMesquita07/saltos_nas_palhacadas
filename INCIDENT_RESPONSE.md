# Plano de Resposta a Incidentes

Processo mínimo para incidentes de segurança, privacidade ou disponibilidade no projeto **Saltos nas Palhaçadas**.

Last verified: 2026-09-16.

## Objetivos

- Conter rapidamente.
- Proteger dados pessoais e media privada.
- Preservar evidências técnicas.
- Restaurar serviço com segurança.
- Decidir, com apoio adequado, se é necessária notificação à CNPD ou titulares.
- Documentar causa e prevenção.

## Contactos

| Função | Contacto |
| --- | --- |
| Responsável técnico | A definir |
| Responsável negócio | A definir |
| Contacto privacidade | `ola@saltosnaspalhacadas.pt` |
| Cloud Run/GCP | Projeto `saltos-prod-gmesquita` |
| Cloudflare | Pages, DNS, TLS, Turnstile, R2 |
| Neon | Projeto `saltos-production` |
| SMTP | Brevo ativo e validado |

## Severidade

| Nível | Exemplo |
| --- | --- |
| Baixo | Falha temporária sem dados pessoais expostos. |
| Médio | Indisponibilidade parcial, falha de email ou job. |
| Alto | Exposição limitada de dados pessoais, media privada ou conta admin suspeita. |
| Crítico | Compromisso de DB, secrets, R2, JWT signing secret ou grande volume de dados. |

## Processo Geral

1. Registar hora, origem, sintomas e impacto inicial.
2. Conter: rollback, desativar funcionalidade, bloquear conta, revogar chave ou limitar tráfego.
3. Preservar logs necessários sem copiar dados pessoais em excesso.
4. Identificar dados, utilizadores e período afetados.
5. Avaliar confidencialidade, integridade e disponibilidade.
6. Rodar secrets afetados.
7. Corrigir em branch própria e validar.
8. Fazer deploy controlado.
9. Confirmar recuperação via smoke tests.
10. Avaliar obrigações RGPD com apoio jurídico.
11. Registar post-mortem e tarefas preventivas.

## Cenários

| Cenário | Ação imediata |
| --- | --- |
| Secret leak | Revogar segredo, criar novo no provider, atualizar Secret Manager, redeploy, auditar acessos. |
| JWT secret comprometido | Rodar `jwt-secret`, redeploy, forçar novo login de utilizadores. |
| DB compromise | Isolar DB, rodar credenciais, preservar logs, restaurar se necessário, avaliar notificação. |
| R2 compromise | Revogar R2 keys, auditar objetos, confirmar bucket policies, restaurar media se possível. |
| Cloudflare incident | Verificar Pages/DNS/R2/Turnstile, comunicar indisponibilidade, usar rollback quando aplicável. |
| Cloud Run incident | Reverter revisão, verificar health/logs, confirmar env/secrets. |
| Upload malicioso | Remover objeto, bloquear conta/IP quando aplicável, rever validação e logs. |
| Conta admin comprometida | Desativar/rodar password admin, rever ações admin, rodar secrets se houve exposição. |
| Turnstile outage | Avaliar impacto em auth pública; manter fail-closed ou decidir mitigação temporária controlada. |
| SMTP outage | Pausar/monitorizar emails e reminders; confirmar se houve marcação indevida de reminders. |

## Dados Que Não Devem Ir Para Logs

- Passwords.
- JWTs.
- Turnstile tokens.
- API keys.
- SMTP password.
- Connection strings completas.
- Conteúdo completo de mensagens sensíveis.
- Telefones completos quando não necessários.

## Checklist Pós-Incidente

- Segredos rodados.
- Password admin revista.
- Tokens/sessões invalidados quando aplicável.
- Logs preservados e minimizados.
- Backups verificados.
- Comunicação preparada.
- Issue preventiva criada.
- Documentação atualizada se a arquitetura/processo mudou.
