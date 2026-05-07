const pool = require('./db');

async function insertMessage({ conversationId, senderId, type, content, mediaUrl }) {
  const [res] = await pool.query(
    `INSERT INTO messages (conversation_id, sender_id, type, content, media_url)
     VALUES (?, ?, ?, ?, ?)`,
    [conversationId, senderId, type || 'text', content || null, mediaUrl || null]
  );
  const [rows] = await pool.query(
    'SELECT id, conversation_id, sender_id, type, content, media_url, created_at FROM messages WHERE id = ?',
    [res.insertId]
  );
  return rows[0];
}

async function recordDelivery(messageId, userId) {
  const [res] = await pool.query(
    'INSERT IGNORE INTO message_deliveries (message_id, user_id) VALUES (?, ?)',
    [messageId, userId]
  );
  return res.affectedRows > 0;
}

async function getMessageSender(messageId) {
  const [rows] = await pool.query(
    'SELECT sender_id FROM messages WHERE id = ?',
    [messageId]
  );
  return rows[0]?.sender_id || null;
}

module.exports = { insertMessage, recordDelivery, getMessageSender };
