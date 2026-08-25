package com.earthy.shop.common.config;

import com.earthy.shop.common.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    // 비밀번호 암호화 객체 생성
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 관리자 API 인증 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .requestMatchers("/api/member/auth/signup").permitAll()
                        .requestMatchers("/api/member/auth/login").permitAll()
                        .requestMatchers("/api/member/auth/find-email").permitAll()
                        .requestMatchers("/api/member/auth/find-password").permitAll()
                        .requestMatchers("/api/member/auth/refresh").permitAll()
                        .requestMatchers("/api/member/auth/logout").permitAll()
                        .requestMatchers("/api/oauth/**").permitAll()

                        // 게시판 공개 API
                        .requestMatchers(HttpMethod.GET, "/api/boards/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/boards/*/password").permitAll()

                        // 공지사항 공개 API
                        .requestMatchers(HttpMethod.GET, "/api/notices/**").permitAll()

                        // 게시판 회원 API
                        .requestMatchers(HttpMethod.POST, "/api/boards").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.PATCH, "/api/boards/**").hasRole("MEMBER")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/member/**").hasRole("MEMBER")
                        .requestMatchers("/api/cart/**").hasRole("MEMBER")
                        .requestMatchers("/api/orders/**").hasRole("MEMBER")
                        .requestMatchers("/api/payments/**").hasRole("MEMBER")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(
                        new JwtFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // 로컬 프론트엔드 요청 허용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174",
                "http://127.0.0.1:5175",
                "https://earthy-shop.com",
                "https://www.earthy-shop.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
