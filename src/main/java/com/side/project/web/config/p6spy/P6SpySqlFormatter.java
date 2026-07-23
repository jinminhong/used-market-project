package com.side.project.web.config.p6spy;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

/**
 * P6Spy가 리플렉션으로 직접 생성하는 클래스이므로 Spring 빈으로 등록할 필요가 없다(public 기본 생성자만 있으면 됨).
 * spy.properties의 logMessageFormat에서 이 클래스를 참조한다.
 */
public class P6SpySqlFormatter implements MessageFormattingStrategy {

    private static final String APP_BASE_PACKAGE = "com.side.project";
    private static final String SELF_PACKAGE = "com.side.project.web.config.p6spy";

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category,
                                 String prepared, String sql, String url) {
        if (sql == null || sql.isBlank()) {
            return "";
        }

        String caller = findCaller();

        if (!"statement".equals(category)) {
            return "[SQL] " + elapsed + "ms | " + category + " | at " + caller + System.lineSeparator() + sql;
        }

        String prettySql = FormatStyle.BASIC.getFormatter().format(sql);
        return "[SQL] " + elapsed + "ms | at " + caller + System.lineSeparator() + prettySql;
    }

    private String findCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.startsWith(APP_BASE_PACKAGE) && !className.startsWith(SELF_PACKAGE)) {
                return className + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
            }
        }
        return "unknown";
    }
}
