package com.example.chat_realtime.dto;

import java.time.Instant;

// DTO dùng để gửi/nhận tin nhắn qua WebSocket (STOMP payload)
// Không map trực tiếp entity Message vì entity có nhiều field nội bộ (deleted_at, ...)
// không nên phơi ra ngoài, và tránh vòng lặp serialize khi entity có quan hệ 2 chiều.
public record MessageDto(

        Long id, // null khi client gửi lên (server sẽ set sau khi lưu DB)

        Long conversationId, // conversation mà tin nhắn thuộc về

        Long senderId, // lấy từ Principal (JWT) ở server, KHÔNG tin client gửi lên field này
        String senderUsername, // hiển thị tiện cho client, set ở server

        String content, // nội dung tin nhắn

        Long replyToId, // null nếu không phải reply, khớp field reply_to_id trong schema

        Instant createdAt // set ở server lúc lưu DB, không nhận từ client
) {
}