package pt.saltosnaspalhacadas.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OpenAiSupportChatClientTests {

    @Test
    void sendsResponsesApiRequestAndExtractsOutputText() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            server.createContext("/v1/responses", exchange -> {
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                String request = new String(requestBody, StandardCharsets.UTF_8);
                assertThat(exchange.getRequestMethod()).isEqualTo("POST");
                assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
                assertThat(request).contains("\"model\":\"gpt-5.4-mini\"");
                assertThat(request).contains("Pergunta fora dos botões");

                byte[] response = """
                        {"output":[{"type":"message","content":[{"type":"output_text","text":"Resposta gerada pela IA."}]}]}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();

            OpenAiSupportChatClient client = new OpenAiSupportChatClient(
                    new ObjectMapper(),
                    true,
                    "test-key",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses",
                    "gpt-5.4-mini",
                    120);

            Optional<String> answer = client.ask("Pergunta fora dos botões", "contexto do site");

            assertThat(answer).contains("Resposta gerada pela IA.");
        } finally {
            stop(server);
        }
    }

    private static void stop(HttpServer server) throws IOException {
        server.stop(0);
    }
}
