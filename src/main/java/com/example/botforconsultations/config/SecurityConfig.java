package com.example.botforconsultations.config;


import com.example.botforconsultations.api.filter.JwtAuthFilter;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions

                        // обработчик на .authenticated эндпоинты
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            String errorJson = String.format(
                                    "{\"status\": %d, \"message\": \"%s\"}",
                                    HttpStatus.UNAUTHORIZED.value(),
                                    "Authentication required. Please provide a valid token."
                            );
                            response.getWriter().write(errorJson);
                        })

                        // обработчик на .hasRole эндпоинты
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            String errorJson = String.format(
                                    "{\"status\": %d, \"message\": \"%s\"}",
                                    HttpStatus.FORBIDDEN.value(),
                                    "Access Denied. You do not have the required permissions1."
                            );
                            response.getWriter().write(errorJson);
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // сваггер
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // поинты admin
                        .requestMatchers("/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

}