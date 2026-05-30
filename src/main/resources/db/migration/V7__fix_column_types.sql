-- Corrige tipos de coluna para alinhar com os mapeamentos do Hibernate 7.
-- CHAR(n) e VARCHAR(n) sao tipos distintos — o validador rejeita a divergencia.

-- cpf: CHAR(11) nao comporta o formato com pontuacao (000.000.000-00 = 14 chars)
ALTER TABLE TB_PERSON ALTER COLUMN cpf TYPE VARCHAR(14);

-- zip_code: CHAR(9) vs VARCHAR(255) esperado pelo Hibernate (campo sem @Column explicito)
ALTER TABLE TB_PERSON ALTER COLUMN zip_code TYPE VARCHAR(255);

-- password: VARCHAR(100) vs VARCHAR(255) esperado pelo Hibernate (sem @Column explicito)
-- Hashes BCrypt tem 60 chars, mas o padrao Hibernate e 255
ALTER TABLE TB_PERSON ALTER COLUMN password TYPE VARCHAR(255);
