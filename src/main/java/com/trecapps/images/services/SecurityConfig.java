package com.trecapps.images.services;

import com.trecapps.auth.webflux.services.TrecSecurityContextReactive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@Configuration
@Order(1)
public class SecurityConfig {

    TrecSecurityContextReactive trecSecurityContext;

    @Autowired
    SecurityConfig(TrecSecurityContextReactive trecSecurityContext1) {
        this.trecSecurityContext = trecSecurityContext1;
    }

    String[] restrictedEndpoints = {
        "/Image-API/**"
    };

    String[] verifiedEndpoints = {
        "/Image-API/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(restrictedEndpoints).authenticated()
                        .pathMatchers(verifiedEndpoints).hasAuthority("TREC_VERIFIED")
                        .pathMatchers(verifiedEndpoints).hasAuthority("MFA_PROVIDED")
                        .anyExchange().permitAll())
                .securityContextRepository(trecSecurityContext)

                .build();
    }


}
