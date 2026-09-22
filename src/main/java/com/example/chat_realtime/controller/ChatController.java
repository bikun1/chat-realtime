package com.example.chat_realtime.controller;

import com.example.chat_realtime.dto.MessageDto;
import com.example.chat_realtime.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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
     * (client subscribe /topic/conversation.{id} để NHẬN, còn gửi thì publish vào
     * đây)
     *
     * Principal: Spring tự inject dựa trên identity đã xác thực lúc handshake
     * WebSocket (cấu hình ở phase JWT auth). Nếu chưa làm security, principal sẽ
     * null → nhớ hoàn thiện security trước khi test thật, method này sẽ NPE ngay
     * ở dòng lấy senderId.
     */
    @MessageMapping("/chat.send/{conversationId}")
    public void sendMessage(@DestinationVariable Long conversationId,
            @Payload MessageDto incoming,
            Principal principal) {

        Long senderId = Long.valueOf(principal.getName());

        // Toàn bộ logic nghiệp vụ (check quyền, lưu DB, broadcast) nằm ở service,
        // controller chỉ điều phối — không viết logic xử lý ở đây.
        messageService.sendMessage(conversationId, senderId, incoming.content(), incoming.replyToId());
    }

    /**
     * Bắt exception ném ra từ các @MessageMapping trong controller này.
     * QUAN TRỌNG: khác REST, nếu không có handler này, exception chỉ bị log ở
     * server — client KHÔNG nhận được thông báo lỗi gì, giao diện sẽ "treo im
     * lặng" mà không hiểu vì sao.
     *
     * @SendToUser gửi lỗi RIÊNG về đúng người gửi request (qua /user/queue/errors),
     *             không broadcast lỗi cho cả phòng chat.
     */
    @MessageExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
    @SendToUser("/queue/errors")
    public String handleBusinessException(Exception ex) {
        // Đây là các lỗi mình TỰ NÉM RA (validate quyền, validate input) nên
        // message đã được viết sẵn để hiển thị an toàn cho user.
        return ex.getMessage();
    }

    // Bắt tất cả các exception còn lại (lỗi hệ thống không lường trước) —
    // KHÔNG trả message gốc ra ngoài để tránh lộ chi tiết nội bộ (stack trace,
    // tên bảng DB...). Message gốc vẫn nên được log lại ở đây (log.error(...))
    // để dev debug, chỉ là không gửi cho client.
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public String handleUnexpectedException(Exception ex) {
        return "Đã có lỗi xảy ra, vui lòng thử lại sau";
    }
}