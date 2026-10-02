ALTER TABLE test_attempts
    ADD COLUMN IF NOT EXISTS legacy_result_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_test_attempts_legacy_result_id
    ON test_attempts(legacy_result_id)
    WHERE legacy_result_id IS NOT NULL;

INSERT INTO test_attempts (
    id,
    user_id,
    test_id,
    start_time,
    end_time,
    score,
    percentage,
    time_taken,
    submitted,
    tab_switch_count,
    created_at,
    legacy_result_id
)
SELECT
    gen_random_uuid(),
    tr.user_id,
    tr.test_id,
    tr.completed_at,
    tr.completed_at,
    tr.score,
    CASE
        WHEN tr.total_questions > 0
            THEN (tr.correct_questions * 100.0) / tr.total_questions
        ELSE 0.0
    END,
    NULL,
    TRUE,
    0,
    tr.completed_at,
    tr.id
FROM test_results tr
WHERE NOT EXISTS (
    SELECT 1
    FROM test_attempts ta
    WHERE ta.legacy_result_id = tr.id
       OR (
            ta.user_id = tr.user_id
            AND ta.test_id = tr.test_id
            AND ta.submitted = TRUE
            AND ta.end_time = tr.completed_at
            AND ta.score = tr.score
       )
);
