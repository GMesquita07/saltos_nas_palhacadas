package pt.saltosnaspalhacadas.backend.support.api;

import java.time.Duration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.security.ClientIpAddress;
import pt.saltosnaspalhacadas.backend.security.IpRateLimiter;
import pt.saltosnaspalhacadas.backend.support.SupportChatService;

@RestController
@RequestMapping("/api/v1/support-chat")
public class SupportChatController {
    private final SupportChatService supportChat;
    private final IpRateLimiter rateLimiter;
    private final int rateLimitPerMinute;

    public SupportChatController(
            SupportChatService supportChat,
            IpRateLimiter rateLimiter,
            @Value("${app.support.chat.rate-limit-per-minute:20}") int rateLimitPerMinute) {
        this.supportChat = supportChat;
        this.rateLimiter = rateLimiter;
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    @PostMapping
    SupportChatResponse ask(HttpServletRequest servletRequest, @Valid @RequestBody SupportChatRequest request) {
        if (!rateLimiter.tryAcquire("support-chat", ClientIpAddress.from(servletRequest), rateLimitPerMinute, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas mensagens em pouco tempo. Tenta novamente dentro de instantes.");
        }

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
