package pt.saltosnaspalhacadas.backend.support.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.support.SupportChatService;

@RestController
@RequestMapping("/api/v1/support-chat")
public class SupportChatController {
    private final SupportChatService supportChat;

    public SupportChatController(SupportChatService supportChat) {
        this.supportChat = supportChat;
    }

    @PostMapping
    SupportChatResponse ask(@Valid @RequestBody SupportChatRequest request) {
        SupportChatService.SupportChatReply reply = supportChat.reply(request.message());
        return new SupportChatResponse(reply.answer(), reply.suggestions());
    }

    record SupportChatRequest(
            @NotBlank(message = "Escreve uma mensagem para o assistente")
            @Size(max = 700, message = "A mensagem pode ter no máximo 700 caracteres")
            String message) {
    }

    record SupportChatResponse(String answer, List<String> suggestions) {
    }
}
