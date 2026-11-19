package com.pastudyhub.user.google;

/**
 * Checks the ID token that Google's sign-in button hands to the browser.
 * An interface so tests can use a fake instead of real Google tokens.
 */
public interface GoogleTokenVerifier {

    /**
     * @throws com.pastudyhub.user.exception.InvalidTokenException if the token is not
     *         a valid, current Google token for this app with a verified email
     */
    GoogleIdentity verify(String idToken);
}
