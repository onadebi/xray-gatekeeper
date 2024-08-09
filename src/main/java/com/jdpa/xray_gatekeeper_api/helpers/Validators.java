package com.jdpa.xray_gatekeeper_api.helpers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

public class Validators {
    public static boolean isXmlFile(MultipartFile file) {
        return MediaType.APPLICATION_XML_VALUE.equals(file.getContentType()) || "text/xml".equals(file.getContentType());
    }

    public static boolean isJsonFile(MultipartFile file) {
        return MediaType.APPLICATION_JSON_VALUE.equals(file.getContentType());
    }

    public static long valueMegabytes(long value) {
        return value * 1024 * 1024;
    }

    public static String extractBearerToken() {
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                return authorizationHeader.substring(7); // Remove "Bearer " prefix
            }
        }
        return null;
    }
}
