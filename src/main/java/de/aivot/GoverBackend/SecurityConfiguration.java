package de.aivot.GoverBackend;

import de.aivot.GoverBackend.entryPoints.AuthEntryPointJwt;
import de.aivot.GoverBackend.filters.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
    private final AuthTokenFilter authenticationJwtTokenFilter;
    private final AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    public SecurityConfiguration(AuthTokenFilter authenticationJwtTokenFilter, AuthEntryPointJwt unauthorizedHandler) {
        this.authenticationJwtTokenFilter = authenticationJwtTokenFilter;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public SecurityFilterChain apiLoginFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .antMatcher("/api/login")
                .build();
    }

    @Bean
    public SecurityFilterChain apiPublicFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .antMatcher("/api/public/**")
                .build();
    }

    @Bean
    public SecurityFilterChain apiCommonFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .antMatcher("/api/**")

                .addFilterBefore(authenticationJwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())

                .exceptionHandling()
                .authenticationEntryPoint(unauthorizedHandler)

                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .antMatcher("/**")
                .build();
    }
}
