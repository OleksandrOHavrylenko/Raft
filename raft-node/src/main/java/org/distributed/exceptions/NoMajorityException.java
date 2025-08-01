package org.distributed.exceptions;

import org.distributed.statemanager.State;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Oleksandr Havrylenko
 **/
public class NoMajorityException extends ResponseStatusException {

    public NoMajorityException(final State state) {
        super(HttpStatus.METHOD_NOT_ALLOWED, String.format("Majority not available in cluster, write declined. Try again later. Current node state = %s", state));
    }
}
