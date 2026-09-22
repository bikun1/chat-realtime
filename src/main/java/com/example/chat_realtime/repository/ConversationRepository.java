package com.example.chat_realtime.repository;

import com.example.chat_realtime.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Dùng để tránh tạo trùng conversation 1-1 giữa 2 user (nhờ UNIQUE
    // constraint trên direct_key ở entity Conversation). Seeder và sau này
    // API "tạo/mở chat 1-1" nên check tồn tại trước bằng method này thay vì
    // save() thẳng rồi bắt exception.
    Optional<Conversation> findByDirectKey(String directKey);
}