CREATE TABLE IF NOT EXISTS survey_responses (
    external_id VARCHAR(200) PRIMARY KEY,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    eligible BOOLEAN NOT NULL,
    assignment_type VARCHAR(20) NOT NULL CHECK (assignment_type IN ('CODE', 'CALCULATIONS', 'TEXT', 'PRESENTATION', 'LAB', 'MIXED', 'OTHER', 'UNKNOWN')),
    allotted_band VARCHAR(20) NOT NULL CHECK (allotted_band IN ('SAME_DAY', 'ONE', 'TWO', 'THREE_FOUR', 'FIVE_SEVEN', 'EIGHT_PLUS', 'UNKNOWN')),
    start_band VARCHAR(20) NOT NULL CHECK (start_band IN ('SAME_DAY', 'ONE', 'TWO', 'THREE_FOUR', 'FIVE_SEVEN', 'EIGHT_PLUS', 'AFTER', 'NOT_STARTED', 'UNKNOWN')),
    submission_status VARCHAR(20) NOT NULL CHECK (submission_status IN ('ON_TIME', 'LATE', 'NOT_SUBMITTED', 'UNKNOWN')),
    extension_status VARCHAR(20) NOT NULL CHECK (extension_status IN ('NO', 'YES', 'UNKNOWN')),
    planning VARCHAR(20) NOT NULL CHECK (planning IN ('WRITTEN', 'MENTAL', 'NONE', 'NOT_STARTED', 'UNKNOWN')),
    difficulty INTEGER CHECK (difficulty BETWEEN 1 AND 5),
    other_deadlines VARCHAR(20) NOT NULL CHECK (other_deadlines IN ('ZERO', 'ONE', 'TWO', 'THREE', 'FOUR_PLUS', 'UNKNOWN')),
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS survey_responses_submitted_at_idx ON survey_responses (submitted_at DESC);
