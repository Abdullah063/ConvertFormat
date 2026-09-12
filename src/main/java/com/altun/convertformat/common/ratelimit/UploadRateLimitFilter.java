package com.altun.convertformat.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UploadRateLimitFilter extends OncePerRequestFilter {

    private static final String UPLOAD_PATH = "/api/v1/conversions";
    private final UploadRateLimiter uploadRateLimiter;

    public UploadRateLimitFilter(UploadRateLimiter uploadRateLimiter) {
        this.uploadRateLimiter = uploadRateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !UPLOAD_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (uploadRateLimiter.tryAcquire(request.getRemoteAddr())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                Long.toString(uploadRateLimiter.retryAfterSeconds())
        );
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"Çok fazla dosya yükleme isteği gönderdiniz","path":"/api/v1/conversions"}
                """);
    }
}
