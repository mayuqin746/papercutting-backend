ALTER TABLE posts
ADD COLUMN image_urls TEXT NULL,
ADD COLUMN show_location BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN location_name VARCHAR(80) NOT NULL DEFAULT '';

-- If the category migration has not been applied in this database yet, run:
-- ALTER TABLE posts
-- ADD COLUMN category VARCHAR(120) NOT NULL DEFAULT '';

-- Optional backfill for old single-image works.
-- UPDATE posts
-- SET image_urls = image_url
-- WHERE (image_urls IS NULL OR image_urls = '')
--   AND image_url IS NOT NULL
--   AND image_url <> '';
