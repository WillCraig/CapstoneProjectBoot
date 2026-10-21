package com.northstar.crm.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The API is the security boundary (the Angular guard is only UX). Stateless JWT resource
 * server: every request carries {@code Authorization: Bearer <jwt>} signed with HS256, and the
 * {@code roles} claim becomes {@code ROLE_AGENT} / {@code ROLE_ADMIN}.
 *
 * <pre>
 * POST   /api/v1/auth/login          anyone (issues the token)
 * GET    /actuator/health/**         anyone (k3s probes, smoke); details never shown
 * DELETE /api/v1/customers/{id}      ADMIN (closing a customer is an administrative action)
 * any    /api/**                     AGENT
 * other  /actuator/**                ADMIN
 * </pre>
 *
 * <p>CSRF is off because nothing authenticates with cookies: a forged cross-site request has no
 * way to attach the bearer header. Failures render as Problem Details via
 * {@link ProblemDetailAuthHandlers}.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CrmSecurityProperties.class)
public class SecurityConfig {

  static final String ROLES_CLAIM = "roles";
  private static final int MIN_SECRET_BYTES = 32;

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http, ProblemDetailAuthHandlers authHandlers)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasRole("ADMIN")
                    .requestMatchers("/api/**").hasRole("AGENT")
                    .anyRequest().denyAll())
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(authHandlers)
                    .accessDeniedHandler(authHandlers))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(authHandlers).accessDeniedHandler(authHandlers));
    return http.build();
  }

  @Bean
  ProblemDetailAuthHandlers problemDetailAuthHandlers() {
    return new ProblemDetailAuthHandlers();
  }

  @Bean
  SecretKey jwtSigningKey(CrmSecurityProperties properties) {
    String secret = properties.jwt() == null ? null : properties.jwt().secret();
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "CRM_JWT_SECRET is not set. Deployed environments read it from the crm-secrets "
              + "Secret; for local development run with SPRING_PROFILES_ACTIVE=local.");
    }
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "CRM_JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes for HS256.");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtSigningKey, CrmSecurityProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    // Default validators check exp/nbf (60 s skew); also pin the issuer we sign with.
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
    return decoder;
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CrmSecurityProperties properties) {
    CorsConfiguration cors = new CorsConfiguration();
    List<String> origins = properties.corsAllowedOrigins();
    cors.setAllowedOrigins(origins == null ? List.of() : origins);
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
    cors.setExposedHeaders(List.of("X-Correlation-Id", "Location"));
    cors.setAllowCredentials(false);
    cors.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }

  static JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName(ROLES_CLAIM);
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return converter;
  }
}
