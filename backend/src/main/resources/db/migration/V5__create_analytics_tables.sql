CREATE TABLE learning_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic VARCHAR(255),
    exam_type VARCHAR(100),
    question_type VARCHAR(100),
    difficulty VARCHAR(50),
    total_questions INT DEFAULT 0,
    correct_questions INT DEFAULT 0,
    wrong_questions INT DEFAULT 0,
    skipped_questions INT DEFAULT 0,
    average_score DOUBLE PRECISION,
    average_percentage DOUBLE PRECISION,
    total_time_spent INT, -- in seconds
    last_activity_date TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, topic, exam_type, question_type, difficulty)
);

CREATE TABLE daily_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL,
    tests_attempted INT DEFAULT 0,
    questions_attempted INT DEFAULT 0,
    evaluations_completed INT DEFAULT 0,
    flashcards_reviewed INT DEFAULT 0,
    time_spent INT DEFAULT 0, -- in seconds
    UNIQUE(user_id, activity_date)
);

-- Optimization Indexes
CREATE INDEX idx_learning_statistics_user_id ON learning_statistics(user_id);
CREATE INDEX idx_learning_statistics_topic ON learning_statistics(topic);
CREATE INDEX idx_learning_statistics_exam_type ON learning_statistics(exam_type);
CREATE INDEX idx_daily_activity_user_id ON daily_activity(user_id);
CREATE INDEX idx_daily_activity_date ON daily_activity(activity_date);
CREATE INDEX idx_daily_activity_user_date ON daily_activity(user_id, activity_date);
