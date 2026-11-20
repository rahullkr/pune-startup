package com.punestartup.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.auth.security.CandidateSessionFilter;
import com.punestartup.auth.service.AuthSessionService;
import com.punestartup.common.security.AdminApiKeyFilter;
import com.punestartup.common.security.JsonAccessDeniedHandler;
import com.punestartup.common.security.JsonAuthenticationEntryPoint;
import com.punestartup.common.web.RequestContext;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final String adminApiKey;
    private final AuthSessionService authSessionService;
    private final List<String> allowedOrigins;

    public SecurityConfig(ObjectMapper objectMapper,
                          @Value("${app.admin.api-key}") String adminApiKey,
                          AuthSessionService authSessionService,
                          @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.objectMapper = objectMapper;
        this.adminApiKey = adminApiKey;
        this.authSessionService = authSessionService;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper)))
                .addFilterBefore(new AdminApiKeyFilter(objectMapper, adminApiKey), AuthorizationFilter.class)
                .addFilterBefore(new CandidateSessionFilter(authSessionService), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/logout", "/api/v1/auth/password").authenticated()
                        .requestMatchers("/api/v1/candidates/**").hasRole("CANDIDATE")
                        .anyRequest().permitAll());
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(RequestContext.REQUEST_ID_HEADER));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
