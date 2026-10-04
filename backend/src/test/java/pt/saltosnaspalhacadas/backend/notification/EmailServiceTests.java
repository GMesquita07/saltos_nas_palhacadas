package pt.saltosnaspalhacadas.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailServiceTests {
    @Test
    void disabledOrUnconfiguredEmailMustNotReportDeliverySuccess() {
        EmailService disabled = new EmailService(false, "smtp.invalid", 25, false, false, "", "", "sender@example.test");
        EmailService unconfigured = new EmailService(true, "", 25, false, false, "", "", "sender@example.test");
        assertThat(disabled.send("client@example.test", "Reminder", "Body")).isFalse();
        assertThat(unconfigured.send("client@example.test", "Reminder", "Body")).isFalse();
    }
}
