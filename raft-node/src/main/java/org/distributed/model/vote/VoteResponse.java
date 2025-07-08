package org.distributed.model.vote;

/**
 * @author Oleksandr Havrylenko
 **/
public record VoteResponse(
        int term,
        boolean voteGranted
) {
}
