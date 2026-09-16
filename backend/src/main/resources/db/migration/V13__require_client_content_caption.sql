UPDATE client_content_posts
SET caption = 'Publicação partilhada pelo cliente.'
WHERE caption IS NULL OR LENGTH(TRIM(caption)) = 0;

ALTER TABLE client_content_posts
    ALTER COLUMN caption SET NOT NULL;

ALTER TABLE client_content_posts
    ADD CONSTRAINT client_content_posts_caption_check CHECK (LENGTH(TRIM(caption)) > 0);
