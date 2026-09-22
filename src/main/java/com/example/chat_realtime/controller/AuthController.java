package com.example.chat_realtime.controller;

import com.example.chat_realtime.dto.AuthRequest;
import com.example.chat_realtime.dto.AuthResponse;
import com.example.chat_realtime.dto.RegisterRequest;
import com.example.chat_realtime.entity.User;
import com.example.chat_realtime.repository.UserRepository;
import com.example.chat_realtime.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        // Trong thực tế nên validate kỹ hơn (email format, độ dài password,
        // check trùng username/email trả lỗi rõ ràng...) - ở đây để tối giản.
        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password())); // hash trước khi lưu

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getId(), saved.getUsername());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        // authenticationManager.authenticate() sẽ tự gọi UserDetailsService +
        // PasswordEncoder để so sánh password. Nếu sai -> tự throw
        // BadCredentialsException (Spring xử lý, mình không cần tự so sánh tay).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        User user = userRepository.findByUsername(req.username())
                .orElseThrow(); // không thể null vì authenticate() đã pass ở trên

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token);
    }
}