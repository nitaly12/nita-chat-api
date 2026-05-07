const express = require('express');
const http = require('http');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { Server } = require('socket.io');
const { markConversationSeen, markLatestSeen } = require('./reads');
const { insertMessage, recordDelivery, getMessageSender } = require('./messages');

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
  cors: { origin: '*', methods: ['GET', 'POST'] },
});

app.use(cors());
app.use(express.json());

const UPLOADS_DIR = path.join(__dirname, 'uploads');
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

app.use('/uploads', express.static(UPLOADS_DIR));

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOADS_DIR),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '.webm';
    const unique = `${Date.now()}-${Math.round(Math.random() * 1e9)}${ext}`;
    cb(null, unique);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 },
});

app.get('/', (req, res) => {
  res.json({ status: 'ok', message: 'Chat backend running' });
});

app.post('/upload', upload.single('audio'), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: 'No audio file uploaded' });
  }
  const url = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
  res.json({ url });
});

io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  socket.on('register', (userId) => {
    if (!userId) return;
    socket.data.userId = String(userId);
    socket.join(`user:${userId}`);
  });

  socket.on('join-conversation', (conversationId) => {
    if (!conversationId) return;
    socket.join(`conv:${conversationId}`);
  });

  socket.on('leave-conversation', (conversationId) => {
    if (!conversationId) return;
    socket.leave(`conv:${conversationId}`);
  });

  socket.on('send-message', async (message, ack) => {
    try {
      if (!message?.conversationId || !message?.senderId) {
        if (typeof ack === 'function') ack({ ok: false, error: 'invalid_payload' });
        return;
      }

      const saved = await insertMessage({
        conversationId: message.conversationId,
        senderId: message.senderId,
        type: message.type,
        content: message.content,
        mediaUrl: message.mediaUrl || message.audioUrl,
      });

      const payload = {
        id: String(saved.id),
        conversationId: saved.conversation_id,
        senderId: saved.sender_id,
        type: saved.type,
        content: saved.content,
        mediaUrl: saved.media_url,
        createdAt: saved.created_at,
        clientTempId: message.clientTempId,
      };

      socket.to(`conv:${message.conversationId}`).emit('receive-message', payload);

      if (typeof ack === 'function') {
        ack({ ok: true, status: 'sent', message: payload });
      }
    } catch (err) {
      console.error('send-message failed:', err);
      if (typeof ack === 'function') ack({ ok: false, error: 'server_error' });
    }
  });

  socket.on('delivered', async (payload, ack) => {
    try {
      const messageId = payload?.messageId;
      const userId = payload?.userId || socket.data.userId;
      if (!messageId || !userId) {
        if (typeof ack === 'function') ack({ ok: false, error: 'invalid_payload' });
        return;
      }

      const senderId = await getMessageSender(messageId);
      if (!senderId || senderId === String(userId)) {
        if (typeof ack === 'function') ack({ ok: true, skipped: true });
        return;
      }

      const inserted = await recordDelivery(messageId, userId);
      if (inserted) {
        io.to(`user:${senderId}`).emit('message-delivered', {
          messageId: String(messageId),
          userId: String(userId),
          deliveredAt: Date.now(),
        });
      }

      if (typeof ack === 'function') ack({ ok: true, inserted });
    } catch (err) {
      console.error('delivered failed:', err);
      if (typeof ack === 'function') ack({ ok: false, error: 'server_error' });
    }
  });

  socket.on('mark-as-seen', async (payload, ack) => {
    try {
      const conversationId = payload?.conversationId;
      const userId = payload?.userId;
      const latestOnly = Boolean(payload?.latestOnly);
      if (!conversationId || !userId) {
        if (typeof ack === 'function') ack({ ok: false, error: 'invalid_payload' });
        return;
      }

      const messageIds = latestOnly
        ? await markLatestSeen(conversationId, userId)
        : await markConversationSeen(conversationId, userId);

      if (messageIds.length > 0) {
        socket.to(`conv:${conversationId}`).emit('messages-seen', {
          conversationId,
          userId,
          messageIds,
          readAt: Date.now(),
        });
      }

      if (typeof ack === 'function') ack({ ok: true, messageIds });
    } catch (err) {
      console.error('mark-as-seen failed:', err);
      if (typeof ack === 'function') ack({ ok: false, error: 'server_error' });
    }
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

const PORT = process.env.PORT || 4000;
server.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
