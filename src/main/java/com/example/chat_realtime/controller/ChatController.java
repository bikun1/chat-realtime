package com.example.chat_realtime.controller;

import com.example.chat_realtime.dto.MessageDto;
import com.example.chat_realtime.entity.Message;
import com.example.chat_realtime.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    private final MessageService messageService;

    public ChatController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Client gửi tin nhắn lên: destination = /app/chat.send/{conversationId}
     * Server xử lý xong sẽ broadcast tới tất cả client đang subscribe
     * /topic/conversation.{id}
     *
     * Lưu ý: @SendTo ở đây KHÔNG hỗ trợ path variable động kiểu {conversationId}
     * như REST,
     * nên mình không dùng @SendTo mà tự inject SimpMessagingTemplate để gửi thủ
     * công
     * (xem bên dưới, cách chuẩn hơn cho case có nhiều "phòng" động).
     */
    @MessageMapping("/chat.send/{conversationId}")
    public void sendMessage(@DestinationVariable Long conversationId,
            @Payload MessageDto incoming,
            Principal principal) {

        // Principal.getName() lấy được nhờ đã cấu hình JWT interceptor ở bước WebSocket
        // security.
        // Nếu bạn CHƯA làm phần security, tạm thời sẽ null -> nhớ làm security trước
        // khi test thật.
        Long senderId = Long.valueOf(principal.getName());

        // Toàn bộ logic nghiệp vụ (check quyền, lưu DB) đẩy xuống service,
        // controller chỉ đóng vai trò điều phối - không viết logic ở đây.
        messageService.sendMessage(conversationId, senderId, incoming.content(), incoming.replyToId());
    }

    /**
     * Bắt exception ném ra từ các @MessageMapping trong controller này.
     * QUAN TRỌNG: khác với REST, nếu không có handler này, exception sẽ chỉ bị log
     * ở server,
     * client sẽ KHÔNG nhận được thông báo lỗi gì cả (im lặng treo).
     *
     * @SendToUser gửi lỗi RIÊNG về đúng người gửi request (qua /user/queue/errors),
     *             không broadcast lỗi cho cả phòng chat.
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception ex) {
        return ex.getMessage();
    }
}