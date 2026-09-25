package com.example.chat_realtime.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    // OncePerRequestFilter đảm bảo filter này chỉ chạy ĐÚNG 1 LẦN mỗi request,
    // kể cả khi có forward/include nội bộ (tránh check token 2 lần thừa thãi).
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Không có header hoặc sai format "Bearer <token>" -> bỏ qua, để request
        // đi tiếp (nếu endpoint đó cần auth thì filter chain của Security sẽ tự
        // chặn ở bước sau vì chưa có Authentication trong context).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // bỏ 7 ký tự "Bearer "

        try {
            Long userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.parseClaims(token).get("username", String.class);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Tạo 1 Authentication "đã xác thực" (không cần password vì token đã
            // chứng minh danh tính rồi), gán vào SecurityContext để phần còn lại
            // của Spring (Controller, @AuthenticationPrincipal...) biết ai đang
            // gọi request này.
            var authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            // Token sai/hết hạn -> KHÔNG throw exception ở đây, chỉ đơn giản không
            // set Authentication. Request sẽ bị Spring Security chặn tự nhiên ở
            // bước authorizeHttpRequests nếu endpoint yêu cầu đăng nhập.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}