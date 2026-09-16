ALTER TABLE profiles ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX profiles_display_index ON profiles (display_order, name, id);
