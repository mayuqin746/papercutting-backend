ALTER TABLE posts
ADD COLUMN category VARCHAR(120) NOT NULL DEFAULT '';

-- Optional backfill if you want old works to show one default category.
-- UPDATE posts
-- SET category = '自由创作'
-- WHERE category = '' OR category IS NULL;
