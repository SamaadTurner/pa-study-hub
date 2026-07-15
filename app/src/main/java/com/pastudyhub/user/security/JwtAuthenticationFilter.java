package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security filter that validates JWT tokens on incoming requests.
 *
 * <p>Runs once per request. Extracts the Bearer token from the Authorization header,
 * validates it, and sets the authentication in the SecurityContext so Spring Security
 * knows the user is authenticated for this request.
 *
 * <p>If no token is present, the request proceeds unauthenticated — it will be rejected
 * by Spring Security's endpoint-level authorization rules if the endpoint requires auth.
 *
 * <p>This filter is used within the user-service itself for its own protected endpoints
 * (e.g., GET /api/v1/users/me). The API Gateway has a separate JwtValidationFilter that
 * validates tokens before routing to any downstream service.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                JwtTokenProvider.JwtClaims claims = jwtTokenProvider.validateAndParse(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                claims.getUserId().toString(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + claims.getRole()))
                        );

                // Store userId and email as details so controllers can access them
                authentication.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (InvalidTokenException e) {
                // Don't set auth — request will fail at endpoint authorization
                log.debug("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the Bearer token from the Authorization header.
     *
     * @param request the HTTP request
     * @return the raw token string, or null if not present
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
