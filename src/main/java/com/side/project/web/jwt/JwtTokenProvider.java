package com.side.project.web.jwt;

import com.side.project.web.login.LoginMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_LOGIN_ID = "loginId";
    private static final String CLAIM_NICKNAME = "nickname";
    // application.properties의 jwt.secret 기본값과 동일한 문자열 - 배포 시 JWT_SECRET 미설정을 감지하기 위한 기준값
    private static final String LOCAL_DEV_DEFAULT_SECRET = "local-dev-only-secret-change-me-32bytes-minimum";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                             @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        if (LOCAL_DEV_DEFAULT_SECRET.equals(secret)) {
            log.warn("JWT_SECRET 환경변수가 설정되지 않아 로컬 전용 기본값으로 서명하고 있습니다. 배포 환경에서는 반드시 JWT_SECRET을 설정하세요.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String createAccessToken(Long memberId, String loginId, String nickname) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_LOGIN_ID, loginId)
                .claim(CLAIM_NICKNAME, nickname)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /** Authorization 헤더(예: "Bearer xxx.yyy.zzz")에서 토큰 문자열만 추출한다. */
    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    public String resolveToken(HttpServletRequest request) {
        return extractBearerToken(request.getHeader("Authorization"));
    }

    /** 서명/만료를 검증하고 클레임에서 LoginMember를 복원한다. 유효하지 않으면 JwtAuthenticationException. */
    public LoginMember parseLoginMember(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long memberId = Long.valueOf(claims.getSubject());
            String loginId = claims.get(CLAIM_LOGIN_ID, String.class);
            String nickname = claims.get(CLAIM_NICKNAME, String.class);
            return new LoginMember(memberId, loginId, nickname);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("유효하지 않은 토큰입니다.");
        }
    }
}
