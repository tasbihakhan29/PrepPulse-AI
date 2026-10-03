DROP TABLE IF EXISTS flashcards;

ALTER TABLE daily_activity
    DROP COLUMN IF EXISTS flashcards_reviewed;
