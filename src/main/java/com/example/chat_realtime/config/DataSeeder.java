package com.example.chat_realtime.config;

import com.example.chat_realtime.entity.Conversation;
import com.example.chat_realtime.entity.ConversationMember;
import com.example.chat_realtime.entity.Message;
import com.example.chat_realtime.entity.User;
import com.example.chat_realtime.repository.ConversationMemberRepository;
import com.example.chat_realtime.repository.ConversationRepository;
import com.example.chat_realtime.repository.MessageRepository;
import com.example.chat_realtime.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner là interface của Spring Boot: bất kỳ bean nào implement
 * nó sẽ tự động được Spring gọi method run() ĐÚNG 1 LẦN, ngay sau khi
 * ApplicationContext khởi tạo xong (tức là sau khi toàn bộ bean, DB
 * connection... đã sẵn sàng), trước khi app bắt đầu nhận request thật.
 * Đây là chỗ lý tưởng để chèn data mẫu lúc dev/test.
 *
 * QUAN TRỌNG: seeder này chỉ nên chạy ở môi trường dev/test, KHÔNG chạy ở
 * production (không ai muốn app production tự chèn user giả mỗi lần
 * deploy). Nếu sau này mày tách profile (application-prod.yaml...), nhớ
 * thêm @Profile("!prod") vào class này để tắt nó ở production.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder; // bean có sẵn từ SecurityConfig, dùng để hash password giống lúc
                                                   // register thật

    public DataSeeder(UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            MessageRepository messageRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Guard quan trọng: nếu DB đã có user rồi thì bỏ qua, không seed lại.
        // Không có dòng này thì MỖI LẦN restart app, seeder sẽ cố tạo lại
        // user trùng username/email -> vỡ UNIQUE constraint -> app crash lúc
        // start. Nhờ dòng này, seeder chỉ thực sự chạy đúng 1 lần (lần đầu
        // tiên trên 1 DB trống).
        if (userRepository.count() > 0) {
            System.out.println("[DataSeeder] DB đã có data, bỏ qua seeding.");
            return;
        }

        System.out.println("[DataSeeder] DB trống, bắt đầu tạo data mẫu...");

        // ---- 1. Tạo 3 user mẫu, password mẫu đều là "123456" ----
        // Dùng đúng passwordEncoder.encode() giống AuthController.register()
        // để login qua /auth/login bằng password thô "123456" vẫn hoạt động
        // bình thường (AuthenticationManager sẽ so sánh hash lúc login).
        User alice = createUser("alice", "alice@test.com", "123456");
        User bob = createUser("bob", "bob@test.com", "123456");
        User charlie = createUser("charlie", "charlie@test.com", "123456");

        // ---- 2. Tạo 1 conversation 1-1 giữa alice và bob ----
        // directKey ghép 2 userId theo thứ tự nhỏ->lớn (vd "1_2") để đảm bảo
        // luôn ra cùng 1 giá trị bất kể ai là người "bắt chuyện" trước —
        // đúng với comment thiết kế ở entity Conversation.java.
        Conversation directChat = new Conversation();
        directChat.setGroup(false);
        directChat.setDirectKey(buildDirectKey(alice.getId(), bob.getId()));
        directChat = conversationRepository.save(directChat);
        addMember(directChat.getId(), alice.getId());
        addMember(directChat.getId(), bob.getId());

        // ---- 3. Tạo 1 group chat gồm cả 3 user ----
        Conversation groupChat = new Conversation();
        groupChat.setGroup(true);
        groupChat.setName("Nhóm test 3 người");
        groupChat = conversationRepository.save(groupChat);
        addMember(groupChat.getId(), alice.getId());
        addMember(groupChat.getId(), bob.getId());
        addMember(groupChat.getId(), charlie.getId());

        // ---- 4. Seed vài message sẵn trong 2 phòng trên, để mày test hiển
        // thị lịch sử chat (không chỉ test gửi mới qua WebSocket) ----
        seedMessage(directChat.getId(), alice.getId(), "Chào Bob!", null);
        seedMessage(directChat.getId(), bob.getId(), "Chào Alice!", null);

        seedMessage(groupChat.getId(), alice.getId(), "Chào cả nhóm!", null);
        Message charlieMsg = seedMessage(groupChat.getId(), charlie.getId(), "Chào Alice, khỏe không?", null);
        // Ví dụ 1 message dạng "reply" — trỏ replyToId về message của charlie ở trên
        seedMessage(groupChat.getId(), bob.getId(), "Mình khỏe, cảm ơn!", charlieMsg.getId());

        System.out.println("[DataSeeder] Xong. userId: alice=" + alice.getId()
                + " bob=" + bob.getId() + " charlie=" + charlie.getId());
        System.out.println("[DataSeeder] conversationId: direct(alice-bob)=" + directChat.getId()
                + " group(3 người)=" + groupChat.getId());
        System.out.println("[DataSeeder] Password cho cả 3 user: 123456");
    }

    private User createUser(String username, String email, String rawPassword) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(u);
    }

    private void addMember(Long conversationId, Long userId) {
        ConversationMember m = new ConversationMember();
        m.setConversationId(conversationId);
        m.setUserId(userId);
        memberRepository.save(m);
    }

    private Message seedMessage(Long conversationId, Long senderId, String content, Long replyToId) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setSenderId(senderId);
        m.setContent(content);
        m.setReplyToId(replyToId);
        return messageRepository.save(m);
    }

    // Luôn ghép id nhỏ trước, id lớn sau -> "1_2" dù truyền vào theo thứ tự nào
    private String buildDirectKey(Long userId1, Long userId2) {
        long a = Math.min(userId1, userId2);
        long b = Math.max(userId1, userId2);
        return a + "_" + b;
    }
}