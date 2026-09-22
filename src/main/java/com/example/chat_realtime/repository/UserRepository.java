package com.example.chat_realtime.repository;

import com.example.chat_realtime.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Dùng khi login (JWT auth) để tìm user theo username
    Optional<User> findByUsername(String username);
}