ALTER TABLE booking_requests
    ADD COLUMN reminder_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX booking_requests_reminder_index
    ON booking_requests (status, event_date, reminder_sent_at);
