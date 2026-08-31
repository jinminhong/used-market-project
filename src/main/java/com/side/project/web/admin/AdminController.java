package com.side.project.web.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ROLE 기반 인가 배선이 실제로 동작하는지 확인하기 위한 최소 엔드포인트.
 * ADMIN 권한이 없으면 SecurityConfig의 hasRole("ADMIN")에서 차단된다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("message", "pong");
    }
}
