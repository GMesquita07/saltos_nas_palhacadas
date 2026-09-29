ALTER TABLE profiles
    ADD COLUMN hero_background_image_position VARCHAR(20) NOT NULL DEFAULT '50% 50%';

ALTER TABLE profiles
    ADD COLUMN hero_background_image_zoom DOUBLE PRECISION NOT NULL DEFAULT 1.0;

ALTER TABLE portfolio_items
    ADD COLUMN thumbnail_position VARCHAR(20) NOT NULL DEFAULT '50% 50%';

ALTER TABLE portfolio_items
    ADD COLUMN thumbnail_zoom DOUBLE PRECISION NOT NULL DEFAULT 1.0;
