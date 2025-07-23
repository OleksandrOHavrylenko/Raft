package org.distributed.grpc;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Oleksandr Havrylenko
 **/
public interface GrpcClient {
    VoteResponse requestVote(final VoteRequest voteRequest, final long timeoutMillis);
    void asyncHeartBeat(final AppendEntriesRequest appendEntriesRequest);
    void asyncReplicateLog(final AppendEntriesRequest appendEntriesRequest, final CountDownLatch writeConcernLatch, final boolean waitForReady);
}
