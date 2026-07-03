CREATE TABLE answer_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    topic VARCHAR(255),
    marks_limit DOUBLE PRECISION NOT NULL,
    file_url TEXT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    evaluation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE answer_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES answer_submissions(id) ON DELETE CASCADE,
    score DOUBLE PRECISION NOT NULL,
    max_marks DOUBLE PRECISION NOT NULL,
    evaluation_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Optimization Indexes
CREATE INDEX idx_answer_submissions_user_id ON answer_submissions(user_id);
CREATE INDEX idx_answer_submissions_created_at ON answer_submissions(created_at DESC);
CREATE INDEX idx_answer_submissions_file_hash ON answer_submissions(file_hash);
CREATE INDEX idx_answer_evaluations_submission_id ON answer_evaluations(submission_id);
