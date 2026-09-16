package pt.saltosnaspalhacadas.backend.support;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.support.chat.rate-limit-per-minute=2")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportChatIntegrationTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void anonymousVisitorsCanAskForBookingHelp() throws Exception {
        mockMvc.perform(post("/api/v1/support-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Preciso de orçamento para um casamento"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", containsString("Agendar")))
                .andExpect(jsonPath("$.answer", containsString("conta criada")))
                .andExpect(jsonPath("$.suggestions", hasItem("Preciso de orçamento")));
    }

    @Test
    void chatMessageCannotBeBlank() throws Exception {
        mockMvc.perform(post("/api/v1/support-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message").value("Escreve uma mensagem para o assistente"));
    }

    @Test
    void anonymousChatIsRateLimitedPerClientIp() throws Exception {
        mockMvc.perform(post("/api/v1/support-chat")
                        .header("X-Forwarded-For", "203.0.113.42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Pergunta livre um"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/support-chat")
                        .header("X-Forwarded-For", "203.0.113.42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Pergunta livre dois"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/support-chat")
                        .header("X-Forwarded-For", "203.0.113.42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Pergunta livre tres"}
                                """))
                .andExpect(status().isTooManyRequests());
    }
}
