package com.example.chat_realtime.repository;

import com.example.chat_realtime.entity.ConversationMember;
import com.example.chat_realtime.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository
        extends JpaRepository<ConversationMember, ConversationMemberId> {

    // Spring Data tự sinh query từ tên method: kiểm tra tồn tại bản ghi
    // với conversationId + userId tương ứng -> dùng để validate quyền trong
    // sendMessage()
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
}