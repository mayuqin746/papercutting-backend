ALTER TABLE posts
ADD COLUMN title VARCHAR(80) NOT NULL DEFAULT '';

UPDATE posts
SET title = LEFT(content, 30)
WHERE title = '' OR title IS NULL;
