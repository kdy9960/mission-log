package com.sparta.missionreport.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.missionreport.global.exception.ExceptionHandlerFilter;
import com.sparta.missionreport.global.jwt.JwtAuthenticationFilter;
import com.sparta.missionreport.global.jwt.JwtAuthorizationFilter;
import com.sparta.missionreport.global.jwt.JwtUtil;
import com.sparta.missionreport.global.jwt.repository.RefreshTokenRepository;
import com.sparta.missionreport.global.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper;

    private final UserDetailsServiceImpl userDetailsServiceImpl;

    private final AuthenticationConfiguration authenticationConfiguration;

    private final RefreshTokenRepository refreshTokenRepository;


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {

        return new JwtAuthorizationFilter(jwtUtil, userDetailsServiceImpl,
                refreshTokenRepository);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        filter.setAuthenticationManager(authenticationManager(authenticationConfiguration));

        return filter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable());

        http.sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                        .requestMatchers("/api/board/**").authenticated()
                        .requestMatchers("/api/card/**").authenticated()
                        .requestMatchers("/api/checklist/**").authenticated()
                        .requestMatchers("/api/column/**").authenticated()
                        .requestMatchers("/api/comment/**").authenticated()
                        .anyRequest().permitAll()
        );

        http.addFilterBefore(new ExceptionHandlerFilter(), JwtAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), JwtAuthorizationFilter.class);

        return http.build();
    }
}
