package com.example.chat_realtime.security;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

// ChannelInterceptor cho phép mình "chen vào" TRƯỚC KHI mỗi frame STOMP
// (CONNECT, SUBSCRIBE, SEND...) được xử lý — dùng đúng lúc CONNECT để check JWT.
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        // StompHeaderAccessor giúp đọc/ghi header + command (CONNECT, SEND...)
        // của frame STOMP đang xử lý.
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Chỉ xử lý auth ở đúng lúc CONNECT — đây là lần duy nhất trong cả vòng
        // đời kết nối mà client "bắt tay" với server, nên chỉ cần verify 1 lần
        // ở đây, Principal sẽ được Spring tự giữ lại cho toàn bộ session sau đó.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // FE sẽ set header tự đặt tên "Authorization" khi connect, ví dụ:
            // stompClient.connect({ Authorization: "Bearer " + token }, ...)
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Không có token -> từ chối luôn kết nối bằng cách throw exception.
                // Spring sẽ đóng kết nối WebSocket, client nhận lỗi handshake.
                throw new IllegalArgumentException("Thiếu token xác thực");
            }

            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.parseClaims(token).get("username", String.class);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Đây là dòng QUAN TRỌNG NHẤT: tạo Principal và gắn vào session
                // WebSocket. Từ đây, MỌI tin nhắn sau này trong cùng session này
                // (kể cả các frame SEND, SUBSCRIBE...) sẽ có Principal.getName()
                // trả về userId — đúng như ChatController đang cần dùng
                // "Long.valueOf(principal.getName())".
                Principal principal = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                accessor.setUser(principal);

            } catch (Exception e) {
                throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
            }
        }

        return message;
    }
}