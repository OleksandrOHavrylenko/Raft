package org.distributed.service.election;

import org.distributed.model.ElectionStatus;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("electionService")
public class ElectionServiceImpl implements ElectionService {
    private static final Logger logger = LoggerFactory.getLogger(ElectionServiceImpl.class);

    private final ClusterInfo clusterInfo;
    private final ExecutorService executor;

    public ElectionServiceImpl(final ClusterInfo clusterInfo) {
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.executor = Executors.newFixedThreadPool(clusterInfo.getOtherNodeCount());
    }

    @Override
    public ElectionStatus startLeaderElection() {
        logger.info("Starting leader election");

//        init with 1, because 1 vote for ourself
        final AtomicInteger electionCounter = new AtomicInteger(1);

        long timeOutMillis = 100;

        VoteRequest voteRequest = new VoteRequest(
                clusterInfo.getCurrentNode().getTerm(),
                clusterInfo.getCurrentNode().getNodeId(),
                clusterInfo.getCurrentNode().getLastLogIndex(),
                clusterInfo.getCurrentNode().getLastLogTerm());

        clusterInfo.getOtherNodes().stream()
                .map(otherNode -> executor.submit(
                        () -> otherNode.getGrpcClient().requestVote(voteRequest, timeOutMillis)))
                .forEach(future -> voteResultProcessing(future, timeOutMillis + 50, electionCounter));

        if (electionCounter.get() >= clusterInfo.getMajoritySize()) {
            return ElectionStatus.ELECTED;
        } else {
            return ElectionStatus.RESTART_ELECTION;
        }
    }

    private void voteResultProcessing(Future<VoteResponse> future, long timeOutMillis, AtomicInteger electionCounter) {
        try {
            VoteResponse voteResponse = future.get(timeOutMillis, TimeUnit.MILLISECONDS);
            if (voteResponse.voteGranted()) {
                electionCounter.incrementAndGet();
            }
        } catch (Exception e) {
            logger.error("Error occurred during leader election", e);
        }
    }
}
