ALTER TABLE app_users ADD COLUMN profile_image_position VARCHAR(32) NOT NULL DEFAULT '50% 50%';
ALTER TABLE app_users ADD COLUMN profile_image_zoom DOUBLE PRECISION NOT NULL DEFAULT 1.0;

DROP INDEX IF EXISTS reviews_profile_public_index;
DROP INDEX IF EXISTS reviews_public_index;

CREATE INDEX reviews_profile_public_date_index
    ON reviews (profile_id, published, review_date DESC, id DESC);
