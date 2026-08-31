package com.side.project.web.login;

import com.side.project.domain.auth.RefreshTokenService;
import com.side.project.domain.member.Member;
import com.side.project.web.SessionConst;
import com.side.project.web.jwt.JwtAuthenticationException;
import com.side.project.web.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.auth.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.auth.cookie.same-site}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginForm loginForm) {
        Member member = loginService.authenticate(loginForm);
        String refreshToken = refreshTokenService.issue(member);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, refreshTokenExpirationMs / 1000).toString())
                .body(toTokenResponse(member));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = SessionConst.REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString())
                .build();
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = SessionConst.REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new JwtAuthenticationException("리프레시 토큰이 없습니다.");
        }
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rotated.rawToken(), refreshTokenExpirationMs / 1000).toString())
                .body(toTokenResponse(rotated.member()));
    }

    private TokenResponse toTokenResponse(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getLoginId(), member.getNickName(), member.getRole());
        return new TokenResponse(accessToken, jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                member.getId(), member.getLoginId(), member.getNickName());
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(SessionConst.REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
