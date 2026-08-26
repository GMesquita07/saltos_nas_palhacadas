package pt.saltosnaspalhacadas.backend.support;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class SupportChatService {

    public SupportChatReply reply(String message) {
        String normalized = normalize(message);

        if (hasAny(normalized, "agend", "marcacao", "marcar", "data", "disponibilidade", "evento", "casamento", "batizado", "aniversario")) {
            return new SupportChatReply(
                    "Para pedir orçamento e agendamento, entra em Agendar, escolhe o artista, a data e preenche os detalhes do evento. É preciso ter conta criada para enviar o pedido. Depois de enviares, o pedido fica em análise e o animador entra em contacto por email ou telemóvel.",
                    List.of("Preciso de orçamento", "Como sei se a data está livre?", "Que dados tenho de preencher?"));
        }

        if (hasAny(normalized, "orcamento", "preco", "valor", "quanto", "pagar", "custo")) {
            return new SupportChatReply(
                    "O orçamento não é calculado automaticamente no site, porque depende do tipo de evento, local, horários, artista e material necessário. Envia um pedido em Agendar e o animador analisa a informação para responder diretamente por email ou telemóvel.",
                    List.of("Fazer pedido de agendamento", "Ver materiais disponíveis", "Contactar a equipa"));
        }

        if (hasAny(normalized, "material", "materiais", "equipamento", "som", "luz", "luzes", "fumo", "maquina")) {
            return new SupportChatReply(
                    "A lista de material disponível está na página Materiais. Lá consegues ver os equipamentos publicados pelo admin, com fotografia e nome, para perceberes o que pode ser usado no evento.",
                    List.of("Ver materiais disponíveis", "Pedir orçamento com material", "Falar com a equipa"));
        }

        if (hasAny(normalized, "contact", "email", "telemovel", "telefone", "whatsapp", "instagram", "morada")) {
            return new SupportChatReply(
                    "Podes ver os contactos públicos na página Contactos. Se já enviaste um pedido de agendamento, usa também o email ou telemóvel que deixaste no pedido para a equipa conseguir acompanhar o teu caso.",
                    List.of("Abrir contactos", "Tenho uma dúvida sobre o pedido", "Preciso de alterar um evento"));
        }

        if (hasAny(normalized, "partilha", "publicacao", "publicar", "foto", "fotografia", "video", "cliente")) {
            return new SupportChatReply(
                    "Na página Partilhas, os clientes com conta podem enviar fotografias ou vídeos dos eventos em que participaram. A publicação só aparece no site depois de ser aprovada pelo admin.",
                    List.of("Como publicar fotos?", "A minha publicação ainda não apareceu", "Tenho de estar logado?"));
        }

        if (hasAny(normalized, "perfil", "perfis", "artista", "dj", "portfolio", "portefolio", "joao", "kidg")) {
            return new SupportChatReply(
                    "Na página Perfis consegues ver os artistas disponíveis. Ao abrir um perfil, encontras o portefólio, vídeo de destaque quando existir, conteúdos publicados e avaliações.",
                    List.of("Ver perfis", "Como escolho um artista?", "Ver avaliações"));
        }

        if (hasAny(normalized, "conta", "login", "entrar", "registar", "registo", "password", "passe", "utilizador")) {
            return new SupportChatReply(
                    "Para criar conta ou iniciar sessão, usa Login / Criar Conta no menu. A conta é necessária para enviar pedidos de agendamento, guardar favoritos, fazer avaliações e submeter partilhas de clientes.",
                    List.of("Criar conta", "Não consigo entrar", "Porque preciso de conta?"));
        }

        if (hasAny(normalized, "favorito", "favoritos", "guardar")) {
            return new SupportChatReply(
                    "Os favoritos permitem guardar artistas para veres mais tarde. Tens de iniciar sessão, abrir o perfil do artista e usar a opção de favorito.",
                    List.of("Ver perfis", "Criar conta", "Onde vejo favoritos?"));
        }

        if (hasAny(normalized, "avaliacao", "avaliacoes", "review", "reviews", "feedback", "estrelas", "comentario")) {
            return new SupportChatReply(
                    "As avaliações são feitas pelos utilizadores nos perfis dos artistas, com comentário e estrelas. Depois, o admin controla quais ficam visíveis no site.",
                    List.of("Como avaliar um artista?", "Ver avaliações", "Preciso de estar logado?"));
        }

        if (hasAny(normalized, "cancelar", "alterar", "mudanca", "mudar", "remarcar")) {
            return new SupportChatReply(
                    "Se precisares de alterar ou cancelar um evento, fala diretamente com a equipa através dos contactos disponíveis. O animador consegue atualizar o estado do evento e deixar uma mensagem de justificação quando necessário.",
                    List.of("Abrir contactos", "Alterar pedido enviado", "Cancelar evento"));
        }

        return new SupportChatReply(
                "Posso ajudar com agendamentos, orçamentos, materiais, perfis de artistas, contactos, partilhas de clientes, favoritos e avaliações. Escreve a tua dúvida ou escolhe uma das opções rápidas.",
                List.of("Pedir orçamento", "Ver materiais", "Contactar a equipa", "Publicar fotos do evento"));
    }

    private static boolean hasAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }

    public record SupportChatReply(String answer, List<String> suggestions) {
    }
}
