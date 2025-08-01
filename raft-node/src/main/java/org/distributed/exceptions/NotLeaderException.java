package org.distributed.exceptions;

import org.distributed.statemanager.State;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Oleksandr Havrylenko
 **/
public class NotLeaderException extends ResponseStatusException {

    public NotLeaderException(final State state) {
        super(HttpStatus.METHOD_NOT_ALLOWED, String.format("Write declined, this node is not a Leader, try other nodes in the cluster. Current node state = %s", state));
    }
}
