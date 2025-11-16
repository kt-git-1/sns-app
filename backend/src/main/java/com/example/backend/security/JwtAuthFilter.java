package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /** 認証不要のパスはフィルタしない（軽量化 & 失敗時の副作用回避） */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        return p.startsWith("/auth/") || p.startsWith("/public/") || p.equals("/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // すでに他で認証済みなら何もしない（他フィルタやテストでセット済みのケースを尊重）
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer "の後ろ
            try {
                // ① 署名/exp を含めてトークンを検証
                Jws<Claims> jws = jwtService.parseToken(token);
                Claims claims = jws.getPayload();

                // ② 誰なのか（sub: userId）を取り出す
                Long userId = Long.valueOf(claims.getSubject());

                // ③ 必要に応じてDBからユーザーや権限を取得
                var userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {

                    User user = userOpt.get();
                    AuthUser authUser = new AuthUser(user.getId(), user.getUsername());

                    // ここでは固定でROLE_USERを付与（必要ならUserのロールから動的に作る）
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

                    var authentication = new UsernamePasswordAuthenticationToken(
                            authUser,                        // principal（ここではusername）
                            null,                            // credentials は持たない
                            authorities                      // 権限
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                    // ④ 認証をコンテキストにセット
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // userが存在しない/非アクティブなら認証しないでスルー（→ 最終的に401）
            } catch (JwtException | IllegalArgumentException e) {
                // 署名不正・期限切れ・形式不正：何もしない（= 未認証のまま）
                // ログに出すならINFOレベル推奨（大量アクセスでログが汚れないように）
            }
        }

        // 次のフィルタ/コントローラへ
        chain.doFilter(req, res);
    }
}
