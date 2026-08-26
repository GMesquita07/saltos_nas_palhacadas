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

@SpringBootTest
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
}
