package pt.saltosnaspalhacadas.backend.maintenance;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pt.saltosnaspalhacadas.backend.booking.BookingReminderService;
import pt.saltosnaspalhacadas.backend.media.ClientContentMediaService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.maintenance.api-key=strong-maintenance-key-with-32-chars")
class MaintenanceControllerTests {
    private static final String HEADER = "X-Maintenance-Key";
    private static final String API_KEY = "strong-maintenance-key-with-32-chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingReminderService bookingReminders;

    @MockitoBean
    private ClientContentMediaService mediaService;

    @BeforeEach
    void resetMocks() {
        reset(bookingReminders, mediaService);
    }

    @Test
    void bookingReminderEndpointWithoutKeyReturnsForbiddenAndDoesNotExecuteService() throws Exception {
        mockMvc.perform(post("/internal/maintenance/booking-reminders"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookingReminders, mediaService);
    }

    @Test
    void bookingReminderEndpointWithWrongKeyReturnsForbiddenAndDoesNotExecuteService() throws Exception {
        mockMvc.perform(post("/internal/maintenance/booking-reminders")
                        .header(HEADER, "wrong-key"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookingReminders, mediaService);
    }

    @Test
    void cleanupEndpointWithWrongKeyReturnsForbiddenAndDoesNotExecuteService() throws Exception {
        mockMvc.perform(post("/internal/maintenance/private-media-cleanup")
                        .header(HEADER, "wrong-key"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookingReminders, mediaService);
    }

    @Test
    void bookingReminderEndpointWithCorrectKeyRunsOnlyBookingReminders() throws Exception {
        when(bookingReminders.sendDueRemindersNow()).thenReturn(3);

        mockMvc.perform(post("/internal/maintenance/booking-reminders")
                        .header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(3));

        verify(bookingReminders).sendDueRemindersNow();
        verifyNoInteractions(mediaService);
    }

    @Test
    void cleanupEndpointWithCorrectKeyRunsOnlyPrivateMediaCleanup() throws Exception {
        when(mediaService.cleanupExpiredPrivateUploadsNow()).thenReturn(2);

        mockMvc.perform(post("/internal/maintenance/private-media-cleanup")
                        .header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        verify(mediaService).cleanupExpiredPrivateUploadsNow();
        verifyNoInteractions(bookingReminders);
    }
}
