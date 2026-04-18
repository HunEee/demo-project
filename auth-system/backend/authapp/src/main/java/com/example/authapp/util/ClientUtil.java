package com.example.authapp.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientUtil {

    // IP 추출 (프록시 고려)
    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 여러 IP 있을 경우 첫 번째 사용
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    // User-Agent
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "Unknown";
    }

    // 디바이스 판별
    public static String getDevice(String userAgent) {
        return getOS(userAgent) + " / " + getBrowser(userAgent);
    }

    // OS 판단 
    private static String getOS(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone") || ua.contains("android")) return "Mobile";
        if (ua.contains("ipad") || ua.contains("tablet")) return "Tablet";
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac")) return "Mac";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }
    
    // 브라우저 판별 
    public static String getBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg")) return "Edge";
        if (ua.contains("chrome") && !ua.contains("edg")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        return "Other";
    }
    
    
}
