# Plano de Recuperação e Backups

Este documento define a estratégia mínima para recuperar o site **Saltos nas Palhaçadas** após falha de infraestrutura, erro de deploy, perda de dados ou incidente.

## Componentes Críticos

| Componente | Conteúdo |
| --- | --- |
| PostgreSQL | Contas, perfis, portfólio, agendamentos, reviews, materiais e metadados. |
| Storage de media | Fotos, vídeos, thumbnails, avatares e conteúdos de clientes. |
| Variáveis de ambiente | Segredos, SMTP, OpenAI, CORS e configuração de produção. |
| Frontend | Build estático publicado. |
| Backend | Imagem Docker/JAR publicado. |

## Backups

### PostgreSQL

- Ativar backups automáticos no provider.
- Confirmar retenção mínima adequada ao negócio.
- Testar restore antes de produção.
- Guardar informação de região e projeto.

### Storage

- Usar storage persistente ou object storage.
- Ativar versioning/lifecycle quando disponível.
- Confirmar que apagar na aplicação apaga ou agenda apagar no storage.

### Segredos

- Guardar segredos apenas no painel do provider ou secret manager.
- Manter cópia segura em gestor de passwords.
- Nunca guardar segredos em Git, README, screenshots ou mensagens.

## Restore

1. Identificar último backup válido.
2. Criar ambiente temporário de recuperação.
3. Restaurar base de dados.
4. Restaurar storage/media.
5. Configurar variáveis de ambiente.
6. Confirmar `/actuator/health`.
7. Testar login admin, páginas públicas, uploads e agendamentos.
8. Redirecionar tráfego só depois dos testes.

## Teste de Recuperação

Executar pelo menos trimestralmente:

- Restore de base de dados para ambiente isolado.
- Validação de uma conta, um perfil, uma review, um agendamento e uma publicação.
- Confirmação de acesso a media pública.
- Confirmação de que media privada continua privada.

## Métricas a Acompanhar

- Erros 5xx.
- Falhas de login.
- Espaço usado em base de dados.
- Espaço usado em storage.
- Falhas de envio de email.
- Falhas do job de lembretes.
- Consumo da OpenAI.
