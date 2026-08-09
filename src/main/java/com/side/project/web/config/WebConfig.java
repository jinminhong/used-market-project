package com.side.project.web.config;

import com.side.project.web.argumentresolver.LoginMemberArgumentResolver;
import com.side.project.web.interceptor.LoginCheckInterceptor;
import com.side.project.web.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor(jwtTokenProvider))
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns(
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
                        "/api/images/**"
                );
    }
}
