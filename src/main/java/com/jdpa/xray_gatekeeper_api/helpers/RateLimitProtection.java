package com.jdpa.xray_gatekeeper_api.helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimitProtection {
    int rateLimit() default 0;
    long rateDuration() default 0;

}
