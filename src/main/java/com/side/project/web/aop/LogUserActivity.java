package com.side.project.web.aop;

import com.side.project.domain.activitylog.ActivityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogUserActivity {

    ActivityType type() default ActivityType.ITEM_VIEW;

    String typeExpr() default "";

    String itemIdExpr();

    String memberIdExpr() default "";
}
