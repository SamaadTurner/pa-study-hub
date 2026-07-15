package com.pastudyhub.user.model;

/**
 * User roles for authorization.
 *
 * <p>STUDENT — standard PA school student. Has access to all study features.
 * ADMIN — content management (creating/editing system questions, managing decks).
 *
 * <p>Roles are checked both at the API Gateway (route-level) and in service
 * layer methods (method-level) for defense in depth.
 */
public enum UserRole {
    STUDENT,
    ADMIN
}
