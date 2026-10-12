-- category deixa de ser ENUM nativo e passa a VARCHAR (os textos dos valores não mudam).
-- Campos obrigatórios viram NOT NULL e os DEFAULTs provisórios da V2 são removidos.
ALTER TABLE transaction_entity
    MODIFY COLUMN category       VARCHAR(40)  NOT NULL,
    MODIFY COLUMN description    VARCHAR(255) NOT NULL,
    MODIFY COLUMN type           VARCHAR(10)  NOT NULL,
    MODIFY COLUMN payment_method VARCHAR(20)  NOT NULL,
    MODIFY COLUMN recurrence     VARCHAR(10)  NOT NULL,
    MODIFY COLUMN status         VARCHAR(10)  NOT NULL,
    MODIFY COLUMN occurred_at    DATE         NOT NULL,
    MODIFY COLUMN created_at     DATETIME(6)  NOT NULL,
    MODIFY COLUMN updated_at     DATETIME(6)  NOT NULL,
    ADD CONSTRAINT chk_transaction_amount_positive CHECK (amount > 0);
