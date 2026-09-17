package pt.saltosnaspalhacadas.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityErrorDispatchTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void directErrorEndpointRequestRemainsBlocked() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalErrorDispatchReturnsServerErrorWithoutStackDetails() throws Exception {
        MvcResult result = mockMvc.perform(get("/error").with(internalErrorDispatch()))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("unexpected upload failure")
                .doesNotContain("IllegalStateException");
    }

    private static RequestPostProcessor internalErrorDispatch() {
        return request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/media");
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("unexpected upload failure"));
            return request;
        };
    }
}
