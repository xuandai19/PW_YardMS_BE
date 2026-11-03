package com.yms.yms.infrastructure.config;

import com.yms.yms.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        var modulePermissionAccess = (org.springframework.security.authorization.AuthorizationManager<RequestAuthorizationContext>)
                (authentication, context) -> {
                    var current = authentication.get();
                    if (current == null || !current.isAuthenticated()) return new AuthorizationDecision(false);
                    Set<String> authorities = current.getAuthorities().stream()
                            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                            .collect(java.util.stream.Collectors.toSet());
                    if (authorities.contains("ROLE_ADMIN") || authorities.contains("*")) {
                        return new AuthorizationDecision(true);
                    }
                    String required = requiredPermission(context.getRequest().getMethod(),
                            context.getRequest().getRequestURI());
                    return new AuthorizationDecision(required == null || authorities.contains(required));
                };
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/ws/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/yard-logs/qr-info"
                        ).permitAll()
                        .anyRequest().access(modulePermissionAccess)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static String requiredPermission(String method, String path) {
        boolean read = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (path.startsWith("/api/users") || path.startsWith("/api/rbac")) return read ? "USER_READ" : "USER_WRITE";
        if (path.startsWith("/api/roles")) return "USER_READ";
        if (path.startsWith("/api/partners")) return read ? "PARTNER_READ" : "PARTNER_WRITE";
        if (path.startsWith("/api/contracts")) return read ? "CONTRACT_READ" : "CONTRACT_WRITE";
        if (path.startsWith("/api/services")) return read ? "SERVICE_READ" : "SERVICE_WRITE";
        if (path.startsWith("/api/service-orders")) return "SERVICE_DISPATCH";
        if (path.startsWith("/api/vehicles")) return "VEHICLE_OPERATE";
        if (path.startsWith("/api/yard-logs")) return "GATE_OPERATE";
        if (path.startsWith("/api/yards")) return read ? "YARD_READ" : "YARD_WRITE";
        if (path.startsWith("/api/trips")) return read ? "TRACKING_READ" : "TRACKING_WRITE";
        if (path.startsWith("/api/tracking")) return read ? "TRACKING_READ" : "TRACKING_WRITE";
        if (path.startsWith("/api/finance")) {
            if (path.contains("/issueable-orders") || path.contains("/interest-config")
                    || path.endsWith("/reconcile")) return "FINANCE_WRITE";
            return read ? "FINANCE_READ" : "FINANCE_WRITE";
        }
        if (path.startsWith("/api/reports")) {
            if (path.contains("/schedules") && !read) return "REPORT_WRITE";
            return read ? "REPORT_READ" : "REPORT_WRITE";
        }
        if (path.startsWith("/api/goods")) return read ? "GOODS_READ" : "GOODS_WRITE";
        if (path.startsWith("/api/confirmations")) return read ? "CONFIRMATION_READ" : "CONFIRMATION_WRITE";
        return null;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
