package pt.saltosnaspalhacadas.backend.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class OpenAiSupportChatClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiSupportChatClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final int maxOutputTokens;

    public OpenAiSupportChatClient(
            ObjectMapper objectMapper,
            @Value("${app.support.ai.enabled:false}") boolean enabled,
            @Value("${app.support.ai.api-key:}") String apiKey,
            @Value("${app.support.ai.endpoint:https://api.openai.com/v1/responses}") String endpoint,
            @Value("${app.support.ai.model:gpt-5.6-luna}") String model,
            @Value("${app.support.ai.max-output-tokens:320}") int maxOutputTokens) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.model = model == null ? "" : model.trim();
        this.maxOutputTokens = maxOutputTokens;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public boolean isConfigured() {
        return enabled && !apiKey.isBlank() && !endpoint.isBlank() && !model.isBlank();
    }

    public Optional<String> ask(String message, String siteContext) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "instructions", instructions(siteContext),
                    "input", message,
                    "max_output_tokens", maxOutputTokens,
                    "store", false,
                    "temperature", 0.2);

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(18))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("OpenAI support chat returned HTTP {}", response.statusCode());
                return Optional.empty();
            }

            return extractText(response.body());
        } catch (IOException exception) {
            LOGGER.warn("OpenAI support chat request failed: {}", exception.getMessage());
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("OpenAI support chat request was interrupted");
            return Optional.empty();
        } catch (RuntimeException exception) {
            LOGGER.warn("OpenAI support chat could not prepare or parse the response: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractText(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return Optional.of(outputText.asText().trim());
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode outputItem : root.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                JsonNode contentText = contentItem.path("text");
                if (contentText.isTextual() && !contentText.asText().isBlank()) {
                    if (!text.isEmpty()) {
                        text.append("\n\n");
                    }
                    text.append(contentText.asText().trim());
                }
            }
        }

        String result = text.toString().trim();
        return result.isBlank() ? Optional.empty() : Optional.of(result);
    }

    private static String instructions(String siteContext) {
        return """
                És o assistente virtual de suporte do site Saltos nas Palhaçadas, em Portugal.
                Responde sempre em português europeu, de forma curta, útil e simpática.
                Usa apenas a informação do contexto abaixo e as regras do site. Não inventes preços, disponibilidade, políticas, datas, contactos ou promessas.
                Se a dúvida envolver orçamento personalizado, confirmação de disponibilidade, alteração/cancelamento de evento ou informação que não esteja no contexto, orienta o cliente para enviar pedido em Agendar ou contactar a equipa.
                Não recolhas dados sensíveis no chat. Para pedidos formais, encaminha para as páginas próprias do site.
                Mantém a resposta com no máximo 4 frases.

                Contexto atual do site:
                %s
                """.formatted(siteContext);
    }

    public List<String> defaultSuggestions() {
        return List.of("Pedir orçamento", "Ver materiais", "Contactar a equipa", "Ver perfis");
    }
}
