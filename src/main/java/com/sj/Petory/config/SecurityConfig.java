package com.sj.Petory.config;

import com.sj.Petory.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers ->
                        headers
                                .frameOptions(
                                        HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .authorizeHttpRequests((authz) ->

                        authz
                                .requestMatchers(HttpMethod.GET, "/api/members").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                                .requestMatchers(
                                        "/api/notification/subscribe",
                                        "/api/members/check-email"
                                        , "/api/members/check-name"
                                        , "/api/members/login"
                                        , "/api/members/guest", "/api/friends/search"
                                        , "/api/pets/species", "/api/pets/breed/**"
                                        , "/api/h2-console/**"
                                        , "/docs/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                                .requestMatchers("/api/oauth/**").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/community/category").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/community/category").hasRole("ADMIN")
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);  // JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 전에 추가

        return http.build();
    }
}
