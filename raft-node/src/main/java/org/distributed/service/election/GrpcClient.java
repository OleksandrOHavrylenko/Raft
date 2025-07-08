package org.distributed.service.election;

import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;

/**
 * @author Oleksandr Havrylenko
 **/
public interface GrpcClient {
    VoteResponse requestVote(final VoteRequest voteRequest, final long timeoutMillis);
}
