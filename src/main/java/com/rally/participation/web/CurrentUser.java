package com.rally.participation.web;

import java.lang.annotation.*;

/**
 * Resolves the caller's user id from the X-User-Id header, which the API Gateway is
 * responsible for setting after validating the caller's JWT. This service trusts that
 * header rather than validating the JWT itself.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
