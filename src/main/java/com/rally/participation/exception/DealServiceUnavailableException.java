package com.rally.participation.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a synchronous call to Deal Service (reserve-slot / check-leave-eligible)
 * cannot be completed at all (timeout, connection refused, 5xx). We deliberately treat
 * this as a failure rather than assuming success, since optimistically inserting a
 * participation row here could create a row with no reserved slot behind it.
 */
public class DealServiceUnavailableException extends ParticipationApiException {
    public DealServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
