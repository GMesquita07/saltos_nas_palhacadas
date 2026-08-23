ALTER TABLE profiles ADD COLUMN profile_image_zoom DOUBLE PRECISION NOT NULL DEFAULT 1.0;

ALTER TABLE app_users ADD COLUMN username VARCHAR(80);
ALTER TABLE app_users ADD COLUMN first_name VARCHAR(80);
ALTER TABLE app_users ADD COLUMN last_name VARCHAR(80);
ALTER TABLE app_users ADD COLUMN phone VARCHAR(30);
ALTER TABLE app_users ADD COLUMN profile_image_url VARCHAR(2048);

UPDATE app_users
SET username = LOWER(SUBSTRING(email FROM 1 FOR POSITION('@' IN email) - 1))
WHERE username IS NULL;
