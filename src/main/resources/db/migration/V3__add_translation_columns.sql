ALTER TABLE chat_message
    ADD COLUMN translated_content VARCHAR(1000),
    ADD COLUMN translation_status VARCHAR(10) NOT NULL DEFAULT 'NONE',
    ADD COLUMN source_language VARCHAR(2),
    ADD COLUMN target_language VARCHAR(2);
