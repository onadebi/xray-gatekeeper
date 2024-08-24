package com.jdpa.xray_gatekeeper_api.xray.services;

import com.jdpa.xray_gatekeeper_api.configurations.AppConfig;
import com.jdpa.xray_gatekeeper_api.helpers.RateLimitException;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.dtos.FilesTransferData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class XRayRateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(XRayRateLimitService.class);
    public static final String ERROR_MESSAGE = "Too many request at endpoint %s from IP %s! Please try again after %d milliseconds!";
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestCounts = new ConcurrentHashMap<>();


    public AppResponse<String> rateLimit(FilesTransferData data) {;

        AppResponse<String> objRep = new AppResponse<String>(data.getId().toString(),null,200, true);
        final long currentTime = System.currentTimeMillis();

        String bearerToken = null;
        if (data != null && data.getToken() != null && !data.getToken().isBlank()) {
            bearerToken = data.getToken();
        }else{
            AppResponse<String> response = AppResponse.failed(null, "No bearer token found in request. 401 - Unauthorized", 401);
            String errMsg = response.toString();
            throw new IllegalArgumentException(errMsg);
        }
        int rateLimit = AppConfig.DEFAULT_RATE_LIMIT;
        long rateDuration = AppConfig.DEFAULT_RATE_DURATION;

        requestCounts.putIfAbsent(bearerToken, new ConcurrentLinkedQueue<>());
        ConcurrentLinkedQueue<Long> timestamps = requestCounts.get(bearerToken);

        timestamps.add(currentTime);
        cleanUpOldRequests(currentTime, rateDuration, bearerToken);

        logger.info("Total Requests in bucket: [{}]", requestCounts.size());
        logger.info("Total Requests for token: [{}] in bucket: [{}]", bearerToken.substring(0, 15), timestamps.size());

        if (timestamps.size() > rateLimit) {
            objRep.setStatCode(HttpStatus.TOO_MANY_REQUESTS.value());
            objRep.setResult("TOO_MANY_REQUESTS");
            objRep.setSuccess(false);
            throw new RateLimitException(String.format(ERROR_MESSAGE, data.getUrl(), bearerToken, rateDuration));
        }
        return objRep;
    }

    private void cleanUpOldRequests(final long currentTime, final long rateDuration, String bearerToken) {
        ConcurrentLinkedQueue<Long> timestamps = requestCounts.get(bearerToken);

        if (timestamps != null) {
            // Remove timestamps outside the rate duration
            while (!timestamps.isEmpty() && timeIsTooOld(currentTime, timestamps.peek(), rateDuration)) {
                timestamps.poll();
            }
            // Optionally remove the token if it has no more requests
            if (timestamps.isEmpty()) {
                requestCounts.remove(bearerToken);
            }
        }
    }

    private boolean timeIsTooOld(final long currentTime, final long timeToCheck, final long rateDuration) {
        return currentTime - timeToCheck > rateDuration;
    }
}
