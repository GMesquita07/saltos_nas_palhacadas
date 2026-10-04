ALTER TABLE booking_requests
    ADD COLUMN artist_reminder_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX booking_requests_artist_reminder_index
    ON booking_requests (status, event_date, artist_reminder_sent_at);
