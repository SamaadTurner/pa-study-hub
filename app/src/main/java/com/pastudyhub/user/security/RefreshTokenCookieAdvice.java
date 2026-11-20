package com.pastudyhub.user.security;

import com.pastudyhub.user.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Every endpoint that signs someone in returns an AuthResponse (login, register,
 * Google, refresh, password reset, password change). Here, in one place, the
 * refresh token is taken out of that JSON and put in the HttpOnly cookie instead.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class RefreshTokenCookieAdvice implements ResponseBodyAdvice<Object> {

    private final RefreshTokenCookie refreshTokenCookie;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AuthResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof AuthResponse auth && auth.getRefreshToken() != null) {
            response.getHeaders().add(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(auth.getRefreshToken()).toString());
            auth.setRefreshToken(null);
        }
        return body;
    }
}
