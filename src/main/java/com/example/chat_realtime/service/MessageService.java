package com.example.chat_realtime.service;

import com.example.chat_realtime.dto.MessageDto;
import com.example.chat_realtime.entity.Conversation;
import com.example.chat_realtime.entity.Message;
import com.example.chat_realtime.entity.User;
import com.example.chat_realtime.repository.ConversationMemberRepository;
import com.example.chat_realtime.repository.MessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    // (userRepository, conversationRepository... tự inject thêm nếu cần load
    // entity)

    public MessageService(MessageRepository messageRepository,
            ConversationMemberRepository memberRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void sendMessage(Long conversationId, Long senderId, String content, Long replyToId) {

        // 1. VALIDATE: sender có phải member của conversation này không.
        // Đây là bước dễ bị quên nhất khi mới học WebSocket - vì không có
        // filter/interceptor
        // theo từng request như REST, ai cũng có thể publish tới bất kỳ destination nào
        // nếu bạn không tự check quyền trong code.
        boolean isMember = memberRepository.existsByConversationIdAndUserId(conversationId, senderId);
        if (!isMember) {
            throw new IllegalStateException("Bạn không phải thành viên của cuộc trò chuyện này");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        // 2. LƯU DB
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setReplyToId(replyToId); // null nếu không reply

        Message saved = messageRepository.save(message);

        // 3. DỰNG DTO TRẢ VỀ từ dữ liệu đã lưu (nguồn tin cậy, không lấy lại từ input
        // client)
        MessageDto dto = new MessageDto(
                saved.getId(),
                saved.getConversationId(),
                saved.getSenderId(),
                null, // TODO: join lấy username nếu cần hiển thị ngay, hoặc để FE tự resolve
                saved.getContent(),
                saved.getReplyToId(),
                saved.getCreatedAt());

        // 4. BROADCAST thủ công tới đúng "phòng" conversationId.
        // Dùng SimpMessagingTemplate thay vì @SendTo vì destination có phần động
        // ({id}).
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, dto);
    }
}