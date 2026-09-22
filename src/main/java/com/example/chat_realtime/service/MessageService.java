package com.example.chat_realtime.service;

import com.example.chat_realtime.dto.MessageDto;
import com.example.chat_realtime.entity.Message;
import com.example.chat_realtime.entity.User;
import com.example.chat_realtime.repository.ConversationMemberRepository;
import com.example.chat_realtime.repository.MessageRepository;
import com.example.chat_realtime.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository; // thêm để lấy username hiển thị
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void sendMessage(Long conversationId, Long senderId, String content, Long replyToId) {

        // 1. VALIDATE quyền: sender có phải member của conversation này không.
        // Đây là bước QUAN TRỌNG vì WebSocket không có filter/interceptor tự động
        // theo từng destination như REST — nếu không tự check, ai cũng publish được
        // vào bất kỳ conversationId nào (kể cả không phải thành viên).
        boolean isMember = memberRepository.existsByConversationIdAndUserId(conversationId, senderId);
        if (!isMember) {
            throw new IllegalStateException("Bạn không phải thành viên của cuộc trò chuyện này");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        // 2. Lấy thông tin sender để hiển thị username ngay cho FE
        // (tránh phải gọi thêm 1 API REST để resolve tên người gửi).
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người gửi"));

        // 3. LƯU DB
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setReplyToId(replyToId); // null nếu không reply

        Message saved = messageRepository.save(message);

        // 4. DỰNG DTO TRẢ VỀ từ dữ liệu đã lưu (nguồn tin cậy — không lấy lại từ input
        // client, tránh trường hợp client tự sửa id/createdAt gửi lên).
        MessageDto dto = new MessageDto(
                saved.getId(),
                saved.getConversationId(),
                saved.getSenderId(),
                sender.getUsername(), // đã resolve thật, không còn null
                saved.getContent(),
                saved.getReplyToId(),
                saved.getCreatedAt());

        // 5. BROADCAST tới đúng "phòng" conversationId.
        // Dùng SimpMessagingTemplate thay vì @SendTo vì destination có phần động
        // ({id}) — @SendTo chỉ hỗ trợ đường dẫn tĩnh, không nội suy được biến.
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, dto);
    }
}