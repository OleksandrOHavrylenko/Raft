package org.distributed.model.vote;

/**
 * @author Oleksandr Havrylenko
 **/
public record VoteRequest(
        long term,
        String candidateId,
        int lastLogIndex,
        long lastLogTerm
) {
}
