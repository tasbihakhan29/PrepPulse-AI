ALTER TABLE generated_questions
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(32);
