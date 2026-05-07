const pool = require('./db');

async function markConversationSeen(conversationId, userId) {
  const conn = await pool.getConnection();
  try {
    const [unread] = await conn.query(
      `SELECT m.id
         FROM messages m
         LEFT JOIN message_reads r
           ON r.message_id = m.id AND r.user_id = ?
        WHERE m.conversation_id = ?
          AND m.sender_id <> ?
          AND r.id IS NULL`,
      [userId, conversationId, userId]
    );

    if (unread.length === 0) return [];

    const values = unread.map((row) => [row.id, userId]);
    await conn.query(
      'INSERT IGNORE INTO message_reads (message_id, user_id) VALUES ?',
      [values]
    );

    return unread.map((row) => String(row.id));
  } finally {
    conn.release();
  }
}

async function markLatestSeen(conversationId, userId) {
  const conn = await pool.getConnection();
  try {
    const [rows] = await conn.query(
      `SELECT id FROM messages
        WHERE conversation_id = ? AND sender_id <> ?
        ORDER BY id DESC LIMIT 1`,
      [conversationId, userId]
    );
    if (rows.length === 0) return [];

    const messageId = rows[0].id;
    const [res] = await conn.query(
      'INSERT IGNORE INTO message_reads (message_id, user_id) VALUES (?, ?)',
      [messageId, userId]
    );
    return res.affectedRows > 0 ? [String(messageId)] : [];
  } finally {
    conn.release();
  }
}

module.exports = { markConversationSeen, markLatestSeen };
