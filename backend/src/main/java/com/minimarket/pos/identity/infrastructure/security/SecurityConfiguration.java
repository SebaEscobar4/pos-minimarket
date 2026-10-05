package com.minimarket.pos.identity.infrastructure.security;

import com.minimarket.pos.identity.application.IdentityPrincipal;
import com.minimarket.pos.identity.application.IdentityProperties;
import com.minimarket.pos.shared.infrastructure.security.SameOriginMutationFilter;
import com.minimarket.pos.shared.infrastructure.security.SecurityProblemWriter;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(IdentityProperties.class)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19_456, 2);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    CookieSerializer sessionCookieSerializer(
            @Value("${POS_COOKIE_SECURE:false}") boolean secureCookie) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("POS_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secureCookie);
        return serializer;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CookieCsrfTokenRepository csrfTokenRepository,
            SameOriginMutationFilter sameOriginMutationFilter,
            SecurityProblemWriter problemWriter)
            throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");

        return http.addFilterBefore(sameOriginMutationFilter, CsrfFilter.class)
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(authorization -> authorization
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sales", "/api/v1/sales/**")
                        .access(this::administratorWithChangedPassword)
                        .requestMatchers("/api/v1/admin/**")
                        .access(this::administratorWithChangedPassword)
                        .requestMatchers("/api/v1/**")
                        .access(this::authenticatedWithChangedPassword)
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> problemWriter.write(
                                request,
                                response,
                                HttpStatus.UNAUTHORIZED.value(),
                                "authentication-required",
                                "Autenticación requerida",
                                "Debes iniciar sesión para acceder a este recurso."))
                        .accessDeniedHandler((request, response, exception) -> problemWriter.write(
                                request,
                                response,
                                HttpStatus.FORBIDDEN.value(),
                                "forbidden",
                                "Acceso denegado",
                                "No tienes permiso para ejecutar esta operación.")))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .build();
    }

    private AuthorizationDecision administratorWithChangedPassword(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return new AuthorizationDecision(administrator && credentialIsFinal(authentication));
    }

    private AuthorizationDecision authenticatedWithChangedPassword(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        return new AuthorizationDecision(
                authentication.isAuthenticated() && credentialIsFinal(authentication));
    }

    private boolean credentialIsFinal(Authentication authentication) {
        return authentication.getPrincipal() instanceof IdentityPrincipal principal
                && !principal.passwordChangeRequired();
    }
}
