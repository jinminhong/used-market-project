package com.side.project.web.security;

import com.side.project.web.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인은 LoginService가 PasswordEncoder로 직접 검증하며 Spring Security의
     * AuthenticationManager를 거치지 않는다. UserDetailsService 빈이 없으면 Boot가
     * 임의 비밀번호로 기본 인메모리 사용자를 자동 생성하므로, 그걸 막기 위한 빈이다.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("사용하지 않음: 로그인은 LoginService에서 직접 처리한다.");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // H2 콘솔은 iframe으로 렌더링되므로 기본 X-Frame-Options(DENY)를 풀어줘야 동작한다.
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/api/login",
                                "/api/logout",
                                "/api/token/refresh",
                                "/api/members",
                                "/api/members/check-id",
                                "/api/members/check-nickname",
                                "/api/members/*/shop",
                                "/css/**",
                                "/error",
                                "/api/images/**",
                                "/h2-console/**",
                                "/ws-chat/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
