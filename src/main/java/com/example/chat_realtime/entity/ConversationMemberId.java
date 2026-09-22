package com.example.chat_realtime.entity;

import java.io.Serializable;
import java.util.Objects;

public class ConversationMemberId implements Serializable {
    private Long conversationId;
    private Long userId;

    public ConversationMemberId() {
    }

    public ConversationMemberId(Long conversationId, Long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConversationMemberId that))
            return false;
        return Objects.equals(conversationId, that.conversationId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }
}