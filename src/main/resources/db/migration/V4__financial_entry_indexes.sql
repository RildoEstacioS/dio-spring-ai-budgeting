CREATE INDEX idx_transaction_occurred_at ON transaction_entity (occurred_at);
CREATE INDEX idx_transaction_type_occurred_at ON transaction_entity (type, occurred_at);
CREATE INDEX idx_transaction_category_occurred_at ON transaction_entity (category, occurred_at);
