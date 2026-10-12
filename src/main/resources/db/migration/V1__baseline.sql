-- Schema v1 do desafio, idêntico ao criado pelo Hibernate (inclusive o ENUM nativo de category).
-- Só é executado em bancos vazios; bancos v1 existentes entram por baseline (versão 1).
CREATE TABLE transaction_entity
(
    id          BINARY(16)                            NOT NULL,
    amount      BIGINT                                NOT NULL,
    category    ENUM ('AUTO', 'GROCERIES', 'PHARMA')  DEFAULT NULL,
    description VARCHAR(255)                          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
