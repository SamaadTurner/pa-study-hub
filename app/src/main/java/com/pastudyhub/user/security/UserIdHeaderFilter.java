package com.pastudyhub.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Filter that extracts the user ID from the authenticated security context
 * and adds it as the X-User-Id header for backward compatibility with
 * controllers that expect this header.
 */
public class UserIdHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            // The principal is the user ID (set by JwtAuthenticationFilter)
            String userId = authentication.getPrincipal().toString();

            // Wrap the request to add the X-User-Id header
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        return userId;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        return Collections.enumeration(Collections.singletonList(userId));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> names = new HashSet<>();
                    Enumeration<String> original = super.getHeaderNames();
                    while (original.hasMoreElements()) {
                        names.add(original.nextElement());
                    }
                    names.add("X-User-Id");
                    return Collections.enumeration(names);
                }
            };

            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
