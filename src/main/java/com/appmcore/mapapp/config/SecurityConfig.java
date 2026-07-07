package com.appmcore.mapapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security configuration.
 *
 * <p>The map page, static assets and Actuator health/info endpoints are served
 * without authentication so the viewer works out of the box. All other requests
 * require authentication. CSRF is disabled for the H2 console during local dev
 * and the console frames are permitted.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/css/**",
                    "/js/**",
                    "/api/v1/map/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/h2-console/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            // H2 console renders inside a frame and posts without a CSRF token.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));

        return http.build();
    }
}
