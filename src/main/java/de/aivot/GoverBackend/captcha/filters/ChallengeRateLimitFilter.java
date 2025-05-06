package de.aivot.GoverBackend.captcha.filters;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChallengeRateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String ip = req.getRemoteAddr();  // TODO: consider using a more reliable way to get the IP address (e.g., X-Forwarded-For header)
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            ((HttpServletResponse) res).setStatus(429);
            res.setContentType("text/plain");
            res.getWriter().write("Too many requests");
        }
    }

    private Bucket newBucket(String ip) {
        Refill refill = Refill.intervally(5, Duration.ofSeconds(20)); // allow a max of 5 requests every 20s
        Bandwidth limit = Bandwidth.classic(5, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}