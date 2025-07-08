package org.distributed.model.vote;

/**
 * @author Oleksandr Havrylenko
 **/
public record VoteRequest(
        int term,
        String candidateId,
        int lastLogIndex,
        int lastLogTerm
) {
}
