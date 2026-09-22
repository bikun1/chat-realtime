package com.example.chat_realtime.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // siết lại ở production
                .withSockJS(); // fallback cho trình duyệt không hỗ trợ WS thuần
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue"); // broker in-memory, đủ để học/demo
        registry.setApplicationDestinationPrefixes("/app"); // prefix cho tin nhắn client gửi lên
        registry.setUserDestinationPrefix("/user"); // cho tin nhắn riêng tư 1-1
    }
}
