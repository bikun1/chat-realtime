package com.example.chat_realtime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    // Đọc secret + thời gian hết hạn từ application.yaml, tự build SecretKey
    // 1 lần lúc khởi động (không tạo lại mỗi request cho đỡ tốn CPU).
    public JwtUtil(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    // Tạo token mới cho 1 user sau khi login thành công.
    // subject = userId (dạng String) -> lúc verify sẽ lấy lại được userId này,
    // dùng làm Principal.getName() ở cả REST lẫn WebSocket.
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username) // thông tin phụ, không dùng để authen
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // Parse + verify chữ ký + hạn dùng của token.
    // Nếu token bị sửa (sai chữ ký) hoặc hết hạn -> ném exception ngay tại đây,
    // nơi gọi chỉ cần bắt exception là biết token invalid.
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }
}