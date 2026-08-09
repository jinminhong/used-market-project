package com.side.project.web.interceptor;

import com.side.project.web.SessionConst;
import com.side.project.web.jwt.JwtAuthenticationException;
import com.side.project.web.jwt.JwtTokenProvider;
import com.side.project.web.login.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 브라우저 네이티브 WebSocket 핸드셰이크는 커스텀 헤더를 실을 수 없으므로,
 * 대신 STOMP CONNECT 프레임의 Authorization 헤더로 JWT를 검증한다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = jwtTokenProvider.extractBearerToken(accessor.getFirstNativeHeader("Authorization"));
            if (token == null) {
                throw new JwtAuthenticationException("로그인이 필요합니다.");
            }

            LoginMember loginMember = jwtTokenProvider.parseLoginMember(token);

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(SessionConst.LOGIN_MEMBER, loginMember);
            }
        }

        return message;
    }
}
