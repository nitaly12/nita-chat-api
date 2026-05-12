\set ON_ERROR_STOP on
\timing on

\echo '== BEFORE: message_reactions duplicates =='
SELECT message_id, user_id, COUNT(*) AS dup
FROM message_reactions
GROUP BY message_id, user_id
HAVING COUNT(*) > 1
ORDER BY dup DESC LIMIT 20;

\echo '== BEFORE: post_reactions duplicates =='
SELECT post_id, user_id, COUNT(*) AS dup
FROM post_reactions
GROUP BY post_id, user_id
HAVING COUNT(*) > 1
ORDER BY dup DESC LIMIT 20;

BEGIN;

ALTER TABLE message_reactions
  DROP CONSTRAINT IF EXISTS uk_message_reactions_msg_user_emoji;

DELETE FROM message_reactions a
USING message_reactions b
WHERE a.message_id = b.message_id
  AND a.user_id    = b.user_id
  AND a.id         < b.id;

ALTER TABLE message_reactions
  DROP CONSTRAINT IF EXISTS uk_message_reactions_msg_user;
ALTER TABLE message_reactions
  ADD CONSTRAINT uk_message_reactions_msg_user
  UNIQUE (message_id, user_id);

ALTER TABLE post_reactions
  DROP CONSTRAINT IF EXISTS uk_post_reactions_post_user_emoji;

DELETE FROM post_reactions a
USING post_reactions b
WHERE a.post_id = b.post_id
  AND a.user_id = b.user_id
  AND a.id      < b.id;

ALTER TABLE post_reactions
  DROP CONSTRAINT IF EXISTS uk_post_reactions_post_user;
ALTER TABLE post_reactions
  ADD CONSTRAINT uk_post_reactions_post_user
  UNIQUE (post_id, user_id);

COMMIT;

\echo '== AFTER: message_reactions duplicates (expect 0 rows) =='
SELECT message_id, user_id, COUNT(*)
FROM message_reactions
GROUP BY message_id, user_id HAVING COUNT(*) > 1;

\echo '== AFTER: post_reactions duplicates (expect 0 rows) =='
SELECT post_id, user_id, COUNT(*)
FROM post_reactions
GROUP BY post_id, user_id HAVING COUNT(*) > 1;

\echo '== Constraints on message_reactions =='
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'message_reactions'::regclass AND contype IN ('u','p');

\echo '== Constraints on post_reactions =='
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'post_reactions'::regclass AND contype IN ('u','p');