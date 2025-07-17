package org.distributed.model.vote;

/**
 * @author Oleksandr Havrylenko
 **/
public record VoteResponse(
        long term,
        boolean voteGranted
) {
}
