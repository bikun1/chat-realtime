package com.example.chat_realtime.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TRUE = chat 1-1, FALSE = group chat
    @Column(name = "is_group", nullable = false)
    private boolean isGroup;

    // Chỉ có giá trị khi is_group = false.
    // direct_key là chuỗi duy nhất ghép từ 2 userId (vd "3_7"), có UNIQUE
    // constraint ở DB
    // -> đảm bảo 2 người dùng chỉ có đúng 1 conversation 1-1, không tạo trùng.
    @Column(name = "direct_key", unique = true)
    private String directKey;

    // Chỉ dùng cho group chat, null nếu là chat 1-1
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String getDirectKey() {
        return directKey;
    }

    public void setDirectKey(String directKey) {
        this.directKey = directKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}