CREATE TABLE IF NOT EXISTS messages (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  conversation_id VARCHAR(64)  NOT NULL,
  sender_id     VARCHAR(64)    NOT NULL,
  type          VARCHAR(32)    NOT NULL DEFAULT 'text',
  content       TEXT           NULL,
  media_url     VARCHAR(1024)  NULL,
  created_at    DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_messages_conv_created (conversation_id, created_at),
  KEY idx_messages_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS message_deliveries (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  message_id   BIGINT UNSIGNED NOT NULL,
  user_id      VARCHAR(64)     NOT NULL,
  delivered_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_delivery_message_user (message_id, user_id),
  KEY idx_deliveries_user (user_id),
  CONSTRAINT fk_deliveries_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS message_reads (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  message_id  BIGINT UNSIGNED NOT NULL,
  user_id     VARCHAR(64)     NOT NULL,
  read_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_message_user (message_id, user_id),
  KEY idx_reads_user (user_id),
  CONSTRAINT fk_reads_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SELECT * FROM message_reads;

SELECT message_id, COUNT(*)
FROM message_reads
GROUP BY message_id
HAVING COUNT(*) > 1;


ALTER TABLE comments
    ADD COLUMN author_id BIGINT;

-- Then, add the Foreign Key constraint to link it to your users table
ALTER TABLE comments
    ADD CONSTRAINT fk_comment_author
        FOREIGN KEY (author_id) REFERENCES users(id);