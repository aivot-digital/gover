package de.aivot.GoverBackend.captcha.filters;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChallengeRateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /*
     * This filter limits the number of requests from a single IP address.
     * It allows a maximum of 5 requests every 20 seconds.
     * If the limit is exceeded, it returns a 429 Too Many Requests response.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String ip = extractClientIp(req);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            HttpServletResponse httpRes = (HttpServletResponse) res;
            httpRes.setStatus(429);
            httpRes.setContentType("text/plain");
            httpRes.getWriter().write("Too many requests");
        }
    }

    /**
     * Extracts the client IP address from the request.
     * This method checks for the "X-Forwarded-For" header first,
     * which is commonly used in reverse proxy setups.
     *
     * @param req The servlet request
     * @return The client IP address
     */
    private String extractClientIp(ServletRequest req) {
        if (req instanceof HttpServletRequest httpReq) {
            String xff = httpReq.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                // use only the first IP of the list (original client)
                return xff.split(",")[0].trim();
            }
        }

        return req.getRemoteAddr();
    }

    /*
     * Creates a new bucket for the given IP address.
     * The bucket allows a maximum of 5 requests every 20 seconds.
     *
     * @param ip The client IP address
     * @return A new bucket with the specified rate limit
     */
    private Bucket newBucket(String ip) {
        Refill refill = Refill.intervally(5, Duration.ofSeconds(20)); // allow a max of 5 requests every 20s
        Bandwidth limit = Bandwidth.classic(5, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}