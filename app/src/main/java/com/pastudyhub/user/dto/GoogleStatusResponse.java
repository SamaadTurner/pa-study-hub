package com.pastudyhub.user.dto;

/**
 * Whether Google sign-in is on, and the client ID the button needs. The client ID
 * is public (it is in every page that shows the button), so sending it is fine.
 */
public record GoogleStatusResponse(boolean enabled, String clientId) {
}
