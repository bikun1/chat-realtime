package com.example.chat_realtime.config;

import com.example.chat_realtime.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(UserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Dùng để hash password lúc register, và so sánh lúc login.
    // KHÔNG BAO GIỜ lưu password dạng plain text vào DB.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Nối UserDetailsService (tự lấy user từ DB) với PasswordEncoder
    // để Spring Security biết cách xác thực username/password lúc login.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // Bean này cần thiết để AuthController có thể tự gọi .authenticate(...)
    // lúc xử lý login (xem AuthController bên dưới).
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì đây là stateless API (JWT), không dùng cookie/session
                // nên không có rủi ro CSRF kiểu form truyền thống.
                .csrf(csrf -> csrf.disable())

                // KHÔNG tạo session ở server — mỗi request phải tự mang token,
                // server không "nhớ" ai đã login trước đó (đúng bản chất JWT).
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Cho phép tự do gọi register/login (chưa có token thì sao gọi
                        // được các API khác), và endpoint handshake WebSocket "/ws/**"
                        // (auth thật cho WebSocket xử lý riêng ở interceptor STOMP, không
                        // qua filter chain HTTP này).
                        .requestMatchers("/auth/**", "/ws/**").permitAll()
                        // Mọi request khác bắt buộc phải có JWT hợp lệ.
                        .anyRequest().authenticated())

                // Gắn filter tự viết (đọc + verify JWT) vào TRƯỚC filter login mặc định
                // của Spring Security, để nó chạy đầu tiên trên mỗi request.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}