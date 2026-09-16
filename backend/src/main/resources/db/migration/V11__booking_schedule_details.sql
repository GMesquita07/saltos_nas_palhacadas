ALTER TABLE booking_requests ADD COLUMN location VARCHAR(180);
ALTER TABLE booking_requests ADD COLUMN contact_email VARCHAR(254);
ALTER TABLE booking_requests ADD COLUMN start_time TIME;
ALTER TABLE booking_requests ADD COLUMN end_time TIME;
ALTER TABLE booking_requests ADD COLUMN wedding_couple_names VARCHAR(180);
ALTER TABLE booking_requests ADD COLUMN custom_event_type VARCHAR(120);
ALTER TABLE booking_requests ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE booking_requests ALTER COLUMN budget DROP NOT NULL;

ALTER TABLE booking_requests DROP CONSTRAINT IF EXISTS booking_requests_budget_check;
ALTER TABLE booking_requests DROP CONSTRAINT IF EXISTS booking_requests_status_check;

ALTER TABLE booking_requests ADD CONSTRAINT booking_requests_status_check CHECK (
    status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'COUNTER_PROPOSED', 'CANCELLED')
);

ALTER TABLE booking_requests ADD CONSTRAINT booking_requests_time_window_check CHECK (
    (start_time IS NULL AND end_time IS NULL)
    OR (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
);

CREATE INDEX booking_requests_profile_date_status_time_index
    ON booking_requests (profile_id, event_date, status, start_time, end_time);
