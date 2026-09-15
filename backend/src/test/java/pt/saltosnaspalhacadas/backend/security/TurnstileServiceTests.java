package pt.saltosnaspalhacadas.backend.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class TurnstileServiceTests {
    private static final String SITEVERIFY_URL = "https://turnstile.example.test/siteverify";

    @Test
    void disabledAllowsRequestsWithoutTokenAndDoesNotCallCloudflare() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileService service = service(builder, false, "secret", "saltos.example.test");

        server.expect(never(), requestTo(SITEVERIFY_URL));

        assertThatCode(() -> service.verify(null, "login", "203.0.113.10"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void enabledAllowsValidSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileService service = service(builder, true, "secret-value", "saltos.example.test");

        server.expect(requestTo(SITEVERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(allOf(
                        containsString("secret=secret-value"),
                        containsString("response=token-value"),
                        containsString("remoteip=203.0.113.10"))))
                .andRespond(withSuccess("""
                        {"success":true,"action":"login","hostname":"saltos.example.test"}
                        """, MediaType.APPLICATION_JSON));

        assertThatCode(() -> service.verify("token-value", "login", "203.0.113.10"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void emptyTokenFailsWithForbidden() {
        TurnstileService service = service(RestClient.builder(), true, "secret-value", "saltos.example.test");

        assertThatThrownBy(() -> service.verify(" ", "login", "203.0.113.10"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unsuccessfulResponseFailsWithForbidden() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileService service = service(builder, true, "secret-value", "saltos.example.test");

        server.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":false,"action":"login","hostname":"saltos.example.test"}
                        """, MediaType.APPLICATION_JSON));

        assertForbidden(() -> service.verify("token-value", "login", "203.0.113.10"));
        server.verify();
    }

    @Test
    void wrongActionFailsWithForbidden() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileService service = service(builder, true, "secret-value", "saltos.example.test");

        server.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":true,"action":"register","hostname":"saltos.example.test"}
                        """, MediaType.APPLICATION_JSON));

        assertForbidden(() -> service.verify("token-value", "login", "203.0.113.10"));
        server.verify();
    }

    @Test
    void wrongHostnameFailsWithForbidden() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileService service = service(builder, true, "secret-value", "saltos.example.test");

        server.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":true,"action":"login","hostname":"evil.example.test"}
                        """, MediaType.APPLICATION_JSON));

        assertForbidden(() -> service.verify("token-value", "login", "203.0.113.10"));
        server.verify();
    }

    @Test
    void emptyResponseFailsWithForbidden() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileService service = service(builder, true, "secret-value", "saltos.example.test");

        server.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertForbidden(() -> service.verify("token-value", "login", "203.0.113.10"));
        server.verify();
    }

    @Test
    void cloudflareUnavailableFailsWithServiceUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileService service = service(builder, true, "secret-value", "saltos.example.test");

        server.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.verify("token-value", "login", "203.0.113.10"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        server.verify();
    }

    @Test
    void missingSecretFailsClosed() {
        TurnstileService service = service(RestClient.builder(), true, "", "saltos.example.test");

        assertThatThrownBy(() -> service.verify("token-value", "login", "203.0.113.10"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static TurnstileService service(RestClient.Builder builder, boolean enabled, String secret, String allowedHostnames) {
        return new TurnstileService(
                builder.build(),
                new ObjectMapper(),
                enabled,
                secret,
                SITEVERIFY_URL,
                allowedHostnames);
    }

    private static void assertForbidden(ThrowingRunnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
