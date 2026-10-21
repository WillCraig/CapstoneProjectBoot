-- Lab 49 consumer idempotency: one row per applied eventId.
-- At-least-once delivery can redeliver an event; the PK makes the second apply a no-op.

CREATE TABLE processed_event (
  event_id       UUID PRIMARY KEY,
  event_type     VARCHAR(64) NOT NULL,
  customer_id    VARCHAR(32) NOT NULL,
  correlation_id VARCHAR(64) NOT NULL,
  processed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
