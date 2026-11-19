package com.pastudyhub.user.google;

/**
 * What the app uses from a checked Google ID token.
 *
 * @param subject   Google's id for the account, never changes
 * @param email     the account's email, already lowercased
 * @param firstName given name, may be null
 * @param lastName  family name, may be null
 */
public record GoogleIdentity(String subject, String email, String firstName, String lastName) {
}
