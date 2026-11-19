package com.pastudyhub.user.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleSignInConfig {

    // Nothing is downloaded from Google until the first sign-in, so this bean is
    // harmless when Google sign-in is turned off (no client ID)
    @Bean
    public GoogleTokenVerifier googleTokenVerifier(@Value("${google.client-id:}") String clientId) {
        return JwtGoogleTokenVerifier.forClientId(clientId);
    }
}
