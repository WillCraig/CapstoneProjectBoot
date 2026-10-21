CREATE TABLE processed_events (
                                  event_id UUID PRIMARY KEY,
                                  processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);