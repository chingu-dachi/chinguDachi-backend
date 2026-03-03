ALTER TABLE chat_message RENAME COLUMN content TO original_content;
ALTER TABLE chat_message RENAME COLUMN source_language TO original_language;
ALTER TABLE chat_message RENAME COLUMN target_language TO translated_language;
