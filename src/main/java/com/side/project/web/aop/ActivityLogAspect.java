package com.side.project.web.aop;

import com.side.project.domain.activitylog.ActivityType;
import com.side.project.domain.activitylog.event.UserActivityEvent;
import com.side.project.web.login.LoginMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ActivityLogAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @AfterReturning(pointcut = "@annotation(logUserActivity)", returning = "result")
    public void logActivity(JoinPoint joinPoint, LogUserActivity logUserActivity, Object result) {
        try {
            StandardEvaluationContext context = buildEvaluationContext(joinPoint, result);

            ActivityType activityType = resolveActivityType(logUserActivity, context);
            Long itemId = evaluate(logUserActivity.itemIdExpr(), context, Long.class);
            Long memberId = resolveMemberId(logUserActivity, context);

            if (itemId == null) {
                log.warn("활동 이력의 itemId를 확인할 수 없어 기록을 건너뜁니다. activityType={}", activityType);
                return;
            }

            eventPublisher.publishEvent(new UserActivityEvent(memberId, itemId, activityType));
        } catch (Exception e) {
            log.warn("활동 이력 기록 중 오류가 발생했습니다. 비즈니스 로직에는 영향을 주지 않습니다.", e);
        }
    }

    private StandardEvaluationContext buildEvaluationContext(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("result", result);
        return context;
    }

    private ActivityType resolveActivityType(LogUserActivity logUserActivity, StandardEvaluationContext context) {
        if (!logUserActivity.typeExpr().isEmpty()) {
            String action = evaluate(logUserActivity.typeExpr(), context, String.class);
            return ActivityType.fromOrderAction(action);
        }
        return logUserActivity.type();
    }

    private Long resolveMemberId(LogUserActivity logUserActivity, StandardEvaluationContext context) {
        if (!logUserActivity.memberIdExpr().isEmpty()) {
            return evaluate(logUserActivity.memberIdExpr(), context, Long.class);
        }
        return resolveMemberIdFromRequest();
    }

    private Long resolveMemberIdFromRequest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginMember member)) {
            return null;
        }
        return member.getMemberId();
    }

    private <T> T evaluate(String expression, StandardEvaluationContext context, Class<T> resultType) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        Expression parsed = expressionParser.parseExpression(expression);
        return parsed.getValue(context, resultType);
    }
}
