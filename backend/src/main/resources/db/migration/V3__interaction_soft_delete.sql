-- Lab 50: deleting an interaction from the UI hides it without erasing the audit row.
ALTER TABLE customer_interaction
  ADD COLUMN deleted_at TIMESTAMPTZ;
