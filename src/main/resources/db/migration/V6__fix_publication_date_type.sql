ALTER TABLE TB_BOOKS
ALTER COLUMN publication_date TYPE date USING publication_date::date;
