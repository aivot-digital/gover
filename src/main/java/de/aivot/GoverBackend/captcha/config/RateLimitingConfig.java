package de.aivot.GoverBackend.captcha.config;

import de.aivot.GoverBackend.captcha.filters.ChallengeRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitingConfig {

    @Bean
    public FilterRegistrationBean<ChallengeRateLimitFilter> challengeRateLimitFilter() {
        FilterRegistrationBean<ChallengeRateLimitFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new ChallengeRateLimitFilter());
        reg.addUrlPatterns("/api/public/captcha/challenge/"); // limiting only this endpoint
        reg.setOrder(1);
        return reg;
    }
}