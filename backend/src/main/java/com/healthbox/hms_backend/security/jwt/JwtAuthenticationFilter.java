package com.healthbox.hms_backend.security.jwt;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);

            if (jwtTokenProvider.validateToken(jwt)) {
                Claims claims = jwtTokenProvider.getClaims(jwt);

                AppUserPrincipal principal = new AppUserPrincipal(
                        claims.get("phno", String.class),
                        claims.getSubject(),
                        Role.valueOf(claims.get("role", String.class)),
                        claims.get("hospitalId", Long.class)
                );

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
                TenantContext.set(principal.getHospitalId(), principal.getRole() == Role.SUPER_ADMIN);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
