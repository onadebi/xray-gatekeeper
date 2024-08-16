package com.jdpa.xray_gatekeeper_api.helpers;

import com.jdpa.xray_gatekeeper_api.configurations.AppConfig;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitImplementation {

    public static final String ERROR_MESSAGE = "To many request at endpoint %s from IP %s! Please try again after %d milliseconds!";
    private final ConcurrentHashMap<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

    @Before("@annotation(limitProtection)")
    public void rateLimit(RateLimitProtection limitProtection) {
        final ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        final long currentTime = System.currentTimeMillis();

        // Extract Authorization Bearer Token
        String authHeader = requestAttributes.getRequest().getHeader("Authorization");
        String bearerToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            bearerToken = authHeader.substring(7);
        }
        if(bearerToken == null || bearerToken.isBlank()) {
            AppResponse<String> response = AppResponse.failed(null, "No bearer token found in request. 401 - Unauthorized", 401);
            String errMsg = response.toString();
            throw new RateLimitException(errMsg);
        }
        int rateLimit = limitProtection.rateLimit() <= 0 ? AppConfig.DEFAULT_RATE_LIMIT : limitProtection.rateLimit();
        long rateDuration = limitProtection.rateDuration() <=0 ? AppConfig.DEFAULT_RATE_DURATION : limitProtection.rateDuration();

        requestCounts.putIfAbsent(bearerToken, new ArrayList<>());
        requestCounts.get(bearerToken).add(currentTime);
        cleanUpRequestCounts(currentTime, rateDuration);
        if (requestCounts.get(bearerToken).size() > rateLimit) {
              throw new RateLimitException(String.format(ERROR_MESSAGE, requestAttributes.getRequest().getRequestURI(), bearerToken, rateDuration));
        }
    }

    private void cleanUpRequestCounts(final long currentTime, final long rateDuration) {
        requestCounts.values().forEach(l -> {
            l.removeIf(t -> timeIsTooOld(currentTime, t, rateDuration));
        });
    }

    private boolean timeIsTooOld(final long currentTime, final long timeToCheck, final long rateDuration) {
        return currentTime - timeToCheck > rateDuration;
    }

}
