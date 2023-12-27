package com.sparta.missionreport.global.jwt;

import com.sparta.missionreport.global.jwt.repository.RefreshTokenRepository;
import com.sparta.missionreport.global.security.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String accessToken = jwtUtil.resolveAccessToken(request);
        String refreshToken = jwtUtil.resolveRefreshToken(request);

        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            Claims info = jwtUtil.getUserInfoFromToken(accessToken);

            // 인증정보에 유저정보 넣기
            String email = info.getSubject();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(email);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } else if (refreshToken != null && jwtUtil.validateToken(refreshToken)) {
            Claims info = jwtUtil.getUserInfoFromToken(refreshToken);
            String username = info.getSubject();
            if (refreshTokenRepository.existsByUsername(username)) {
                String newAccessToken = jwtUtil.createAccessToken(username);
                String currentRefreshToken = JwtUtil.BEARER_PREFIX + refreshToken;
                jwtUtil.addJwtToHeader(newAccessToken, currentRefreshToken, response);
                jwtUtil.validateTokenAndThrow(accessToken);
            }
        } else if (accessToken != null) {
            jwtUtil.validateTokenAndThrow(accessToken);
        }

        filterChain.doFilter(request, response);
    }
}
