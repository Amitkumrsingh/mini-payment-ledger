package com.example.paymentledger.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends org.springframework.web.filter.OncePerRequestFilter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String HEADER = "X-Correlation-ID";
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank() || id.length() > 100) id = UUID.randomUUID().toString();
        MDC.put("correlationId", id); response.setHeader(HEADER, id);
        long started = System.nanoTime();
        try { chain.doFilter(request, response); }
        finally {
            log.info("request method={} path={} status={} durationMs={}", request.getMethod(), request.getRequestURI(), response.getStatus(), (System.nanoTime()-started)/1_000_000);
            MDC.remove("correlationId");
        }
    }
}
