CREATE TABLE media_objects (
    id UUID PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attached_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT media_objects_status_check CHECK (status IN ('PENDING', 'ATTACHED', 'PUBLIC', 'DELETED')),
    CONSTRAINT media_objects_size_check CHECK (size_bytes >= 0)
);

CREATE INDEX media_objects_owner_status_index
    ON media_objects (owner_user_id, status, created_at);

CREATE INDEX media_objects_status_created_index
    ON media_objects (status, created_at);

ALTER TABLE client_content_posts
    ADD COLUMN media_object_id UUID;

ALTER TABLE client_content_posts
    ADD COLUMN thumbnail_object_id UUID;

ALTER TABLE client_content_posts
    ADD COLUMN public_display_name VARCHAR(80) NOT NULL DEFAULT 'Cliente';

ALTER TABLE client_content_posts
    ADD COLUMN show_location BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE client_content_posts
    ADD COLUMN show_event_date BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE client_content_posts
    ADD COLUMN consent_version VARCHAR(40);

ALTER TABLE client_content_posts
    ADD COLUMN consented_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE client_content_posts
    ADD CONSTRAINT client_content_posts_media_object_fk
    FOREIGN KEY (media_object_id) REFERENCES media_objects(id) ON DELETE SET NULL;

ALTER TABLE client_content_posts
    ADD CONSTRAINT client_content_posts_thumbnail_object_fk
    FOREIGN KEY (thumbnail_object_id) REFERENCES media_objects(id) ON DELETE SET NULL;
