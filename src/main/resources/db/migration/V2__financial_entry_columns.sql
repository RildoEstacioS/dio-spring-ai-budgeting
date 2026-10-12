-- Colunas do modelo financeiro v2. Os DEFAULTs são provisórios (removidos na V3) e servem ao backfill da v1.
-- Backfill aprovado para registros v1: EXPENSE, OTHER, NONE, CONFIRMED, notes nulo,
-- occurred_at = 2026-10-12 (data de backfill da migração, não a data real de ocorrência)
-- e created_at/updated_at = instante da migração (UTC).
ALTER TABLE transaction_entity
    ADD COLUMN type           VARCHAR(10)  NOT NULL DEFAULT 'EXPENSE',
    ADD COLUMN payment_method VARCHAR(20)  NOT NULL DEFAULT 'OTHER',
    ADD COLUMN recurrence     VARCHAR(10)  NOT NULL DEFAULT 'NONE',
    ADD COLUMN status         VARCHAR(10)  NOT NULL DEFAULT 'CONFIRMED',
    ADD COLUMN notes          VARCHAR(500) NULL,
    ADD COLUMN occurred_at    DATE         NULL,
    ADD COLUMN created_at     DATETIME(6)  NULL,
    ADD COLUMN updated_at     DATETIME(6)  NULL;

UPDATE transaction_entity
SET occurred_at = '2026-10-12',
    created_at  = UTC_TIMESTAMP(6),
    updated_at  = UTC_TIMESTAMP(6)
WHERE occurred_at IS NULL;
