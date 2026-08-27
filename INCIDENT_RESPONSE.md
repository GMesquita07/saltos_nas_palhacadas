# Plano de Resposta a Incidentes

Este documento define o processo mínimo para responder a incidentes de segurança ou privacidade no projeto **Saltos nas Palhaçadas**.

## Objetivos

- Conter o incidente rapidamente.
- Proteger clientes, administradores e dados pessoais.
- Preservar evidências técnicas.
- Decidir se é necessário notificar a CNPD ou titulares dos dados.
- Documentar a causa e evitar repetição.

## Contactos Internos

| Função | Contacto |
| --- | --- |
| Responsável técnico | A definir |
| Responsável pelo negócio | A definir |
| Contacto de privacidade | ola@saltosnaspalhacadas.pt |
| Provider de alojamento | A definir |
| Provider SMTP | A definir |
| Provider IA | OpenAI |

## Severidade

| Nível | Exemplo |
| --- | --- |
| Baixo | Falha sem dados pessoais afetados. |
| Médio | Exposição limitada, sem passwords/tokens. |
| Alto | Exposição de dados pessoais, abuso de conta admin ou media privada. |
| Crítico | Acesso não autorizado a base de dados, segredos, tokens ou grande volume de dados. |

## Processo

1. Detetar e registar hora, origem e descrição inicial.
2. Conter o incidente: bloquear conta, desligar funcionalidade, revogar segredo ou travar deploy.
3. Preservar logs e evidências relevantes sem copiar dados desnecessários.
4. Identificar dados afetados, utilizadores afetados e intervalo temporal.
5. Avaliar impacto em confidencialidade, integridade e disponibilidade.
6. Revogar segredos afetados: `JWT_SECRET`, SMTP, OpenAI, base de dados ou storage.
7. Corrigir a causa técnica em branch própria.
8. Validar correção em staging.
9. Fazer deploy controlado.
10. Decidir se é necessário notificar CNPD e titulares.
11. Documentar o incidente e ações tomadas.
12. Fazer post-mortem e criar tarefas preventivas.

## Dados Que Não Devem Ser Colocados em Logs

- Passwords.
- Tokens JWT.
- API keys.
- SMTP password.
- Descrições completas de pedidos.
- Conteúdo completo do chatbot.
- Telefones completos.
- Dados bancários ou documentos, caso venham a existir.

## Checklist Pós-Incidente

- Segredos rodados.
- Admin password alterada se necessário.
- Tokens/sessões invalidados quando aplicável.
- Logs revistos.
- Backups verificados.
- Comunicação preparada.
- Issue de prevenção criada.
- README/checklists atualizados se necessário.
