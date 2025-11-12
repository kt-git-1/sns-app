package com.example.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
@EnableWebSecurity
public class SecurityConfig {

    // セキュリティのエラーハンドラー
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler,
            JwtAuthFilter jwtAuthFilter
    ) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // パスワードエンコーダー
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptを使うエンコーダーを返す
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF：SPA + JWT の場合はまず無効化でOK
            .csrf(csrf -> csrf.disable())

            // セッション：サーバ側に状態を持たない
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 認可ルール
            .authorizeHttpRequests(auth -> auth
                    // 静的/公開エンドポイント
                    .requestMatchers("/auth/**").permitAll()
                    // それ以外は認証必須
                    .anyRequest().authenticated()
            )

            // 例外ハンドラ
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authenticationEntryPoint)  // 401用
                    .accessDeniedHandler(accessDeniedHandler)            // 403用
            )

            // CORS（Next.js など別オリジンから呼ぶ場合）
            .cors(Customizer.withDefaults())

            // フィルタ差し込み：UsernamePasswordAuthenticationFilter の前
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 詳細設定
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:3000")); // フロントURL
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));
        cfg.setAllowCredentials(true); // Cookie(Refresh)を送受信するなら必須
        cfg.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
