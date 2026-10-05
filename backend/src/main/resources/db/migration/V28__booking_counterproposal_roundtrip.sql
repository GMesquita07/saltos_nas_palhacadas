ALTER TABLE booking_requests ADD COLUMN counter_proposed_by VARCHAR(16);
ALTER TABLE booking_requests ADD COLUMN counter_start_time TIME;
ALTER TABLE booking_requests ADD COLUMN counter_end_time TIME;
ALTER TABLE booking_requests ADD COLUMN customer_in_app_reminder_sent_at TIMESTAMP WITH TIME ZONE;

UPDATE booking_requests
SET counter_proposed_by = 'ADMIN'
WHERE status = 'COUNTER_PROPOSED'
  AND counter_proposed_by IS NULL;

ALTER TABLE booking_requests ADD CONSTRAINT booking_requests_counter_author_check CHECK (
    (status = 'COUNTER_PROPOSED' AND counter_proposed_by IN ('ADMIN', 'CUSTOMER'))
    OR (status <> 'COUNTER_PROPOSED' AND counter_proposed_by IS NULL)
);

ALTER TABLE booking_requests ADD CONSTRAINT booking_requests_counter_time_window_check CHECK (
    (counter_start_time IS NULL AND counter_end_time IS NULL)
    OR (counter_start_time IS NOT NULL AND counter_end_time IS NOT NULL AND counter_start_time < counter_end_time)
);

CREATE INDEX booking_requests_customer_in_app_reminder_index
    ON booking_requests (status, event_date, customer_in_app_reminder_sent_at);
