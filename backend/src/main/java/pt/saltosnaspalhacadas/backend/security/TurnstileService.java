package pt.saltosnaspalhacadas.backend.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class TurnstileService {
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final String GENERIC_FORBIDDEN_MESSAGE = "Validação anti-bot falhou.";
    private static final String GENERIC_UNAVAILABLE_MESSAGE = "Proteção anti-bot temporariamente indisponível.";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String secret;
    private final String siteverifyUrl;
    private final Set<String> allowedHostnames;

    @Autowired
    public TurnstileService(
            ObjectMapper objectMapper,
            @Value("${app.security.turnstile.enabled:false}") boolean enabled,
            @Value("${app.security.turnstile.secret:}") String secret,
            @Value("${app.security.turnstile.siteverify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String siteverifyUrl,
            @Value("${app.security.turnstile.allowed-hostnames:}") String allowedHostnames) {
        this(RestClient.create(), objectMapper, enabled, secret, siteverifyUrl, allowedHostnames);
    }

    TurnstileService(
            RestClient restClient,
            ObjectMapper objectMapper,
            boolean enabled,
            String secret,
            String siteverifyUrl,
            String allowedHostnames) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.secret = clean(secret);
        this.siteverifyUrl = clean(siteverifyUrl);
        this.allowedHostnames = parseAllowedHostnames(allowedHostnames);
    }

    public void verify(String token, String expectedAction, String remoteIp) {
        if (!enabled) {
            return;
        }

        if (secret.isBlank() || siteverifyUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, GENERIC_UNAVAILABLE_MESSAGE);
        }

        String normalizedToken = clean(token);
        if (normalizedToken.isBlank() || normalizedToken.length() > MAX_TOKEN_LENGTH) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, GENERIC_FORBIDDEN_MESSAGE);
        }

        SiteverifyResponse response = siteverify(normalizedToken, remoteIp);
        if (response == null
                || !Boolean.TRUE.equals(response.success())
                || !expectedAction.equals(response.action())
                || !allowedHostnames.contains(clean(response.hostname()).toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, GENERIC_FORBIDDEN_MESSAGE);
        }
    }

    private SiteverifyResponse siteverify(String token, String remoteIp) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);

        String cleanedRemoteIp = clean(remoteIp);
        if (!cleanedRemoteIp.isBlank() && !"unknown".equalsIgnoreCase(cleanedRemoteIp)) {
            form.add("remoteip", cleanedRemoteIp);
        }

        String body;
        try {
            body = restClient.post()
                    .uri(siteverifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException | RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, GENERIC_UNAVAILABLE_MESSAGE);
        }

        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, GENERIC_FORBIDDEN_MESSAGE);
        }

        try {
            return parseSiteverifyResponse(body);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, GENERIC_FORBIDDEN_MESSAGE);
        }
    }

    private SiteverifyResponse parseSiteverifyResponse(String body) {
        JsonNode root = objectMapper.readTree(body);
        JsonNode success = root.path("success");
        JsonNode action = root.path("action");
        JsonNode hostname = root.path("hostname");
        return new SiteverifyResponse(
                success.isBoolean() ? success.booleanValue() : null,
                action.isTextual() ? action.asText() : "",
                hostname.isTextual() ? hostname.asText() : "");
    }

    private static Set<String> parseAllowedHostnames(String rawHostnames) {
        return Arrays.stream(clean(rawHostnames).split(","))
                .map(TurnstileService::clean)
                .filter(hostname -> !hostname.isBlank())
                .map(hostname -> hostname.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record SiteverifyResponse(Boolean success, String action, String hostname) {
    }
}
