package com.geeknito.LMS_backend.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieConfig {

    @Value("${cookie.secure:false}")
    private boolean defaultSecure;

    public static final String COOKIE_NAME = "jwt";

    public void createCookie(HttpServletResponse response, String token, int maxAgeInSeconds, boolean isSecure) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(isSecure || defaultSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeInSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearCookie(HttpServletResponse response, boolean isSecure) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecure || defaultSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
