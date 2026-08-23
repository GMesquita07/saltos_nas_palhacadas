ALTER TABLE reviews ADD COLUMN profile_id BIGINT;
ALTER TABLE reviews ADD COLUMN user_id BIGINT;

ALTER TABLE reviews
    ADD CONSTRAINT reviews_profile_fk
    FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE reviews
    ADD CONSTRAINT reviews_user_fk
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE SET NULL;

DROP INDEX IF EXISTS reviews_public_index;

CREATE INDEX reviews_profile_public_index
    ON reviews (profile_id, published, display_order, review_date DESC);
