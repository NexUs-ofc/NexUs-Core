package com.example.nexuscore.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentProfileResolver {

    public Long profileId() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("Nenhum profile autenticado no contexto atual");
        }
        Jwt jwt = auth.getToken();
        return Long.parseLong(jwt.getSubject());
    }
}
