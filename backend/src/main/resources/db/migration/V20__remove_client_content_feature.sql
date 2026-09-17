DELETE FROM media_objects
WHERE purpose = 'CLIENT_CONTENT';

DROP TABLE IF EXISTS client_content_posts;

ALTER TABLE media_objects
    DROP CONSTRAINT IF EXISTS media_objects_purpose_check;

ALTER TABLE media_objects
    ALTER COLUMN purpose SET DEFAULT 'PROFILE_AVATAR';

ALTER TABLE media_objects
    ADD CONSTRAINT media_objects_purpose_check CHECK (purpose IN ('PROFILE_AVATAR'));
