package com.side.project.domain.auth;

import com.side.project.domain.member.Member;
import com.side.project.web.jwt.JwtAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public String issue(Member member) {
        String rawToken = generateRawToken();
        LocalDateTime expiresAt = LocalDateTime.now().plus(refreshTokenExpirationMs, java.time.temporal.ChronoUnit.MILLIS);
        refreshTokenRepository.save(new RefreshToken(member, hash(rawToken), expiresAt));
        return rawToken;
    }

    // 재사용 탐지 시 revokeAll()로 무효화한 뒤 예외를 던지는데, 기본 rollback 정책이면 그 무효화 자체가
    // 롤백되어 버려 재사용 방어가 무력화된다. 이 예외 타입에 한해 롤백하지 않도록 명시한다.
    @Transactional(noRollbackFor = JwtAuthenticationException.class)
    public Rotated rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new JwtAuthenticationException("유효하지 않은 리프레시 토큰입니다."));

        if (current.isRevoked()) {
            // 이미 폐기된 토큰이 다시 제출됨 - 탈취/재생 공격 정황으로 보고 해당 회원의 모든 토큰을 즉시 무효화한다.
            revokeAll(current.getMember().getId());
            throw new JwtAuthenticationException("리프레시 토큰 재사용이 감지되어 모든 로그인 세션이 종료되었습니다.");
        }
        if (current.isExpired()) {
            throw new JwtAuthenticationException("만료된 리프레시 토큰입니다.");
        }

        current.revoke();
        Member member = current.getMember();
        String newRawToken = issue(member);
        return new Rotated(member, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAll(Long memberId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByMember_IdAndRevokedAtIsNull(memberId);
        tokens.forEach(RefreshToken::revoke);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public record Rotated(Member member, String rawToken) {}
}
