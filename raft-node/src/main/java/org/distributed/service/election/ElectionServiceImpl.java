package org.distributed.service.election;

import org.distributed.model.ElectionStatus;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.distributed.statemanager.BaseState.VOTE_TIMEOUT_MILLIS;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("electionService")
public class ElectionServiceImpl implements ElectionService {
    private static final Logger logger = LoggerFactory.getLogger(ElectionServiceImpl.class);

    private final ClusterInfo clusterInfo;
    private List<Future<VoteResponse>> voteTimeOutHandler;
    private final ExecutorService executor;

    public ElectionServiceImpl(final ClusterInfo clusterInfo) {
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.executor = Executors.newFixedThreadPool(clusterInfo.getOtherNodeCount());
    }

    @Override
    public ElectionStatus startLeaderElection() {
        logger.info("Starting leader election");

//        init with 1, because 1 vote for ourself
        final AtomicInteger voteCounter = new AtomicInteger(1);
        final AtomicInteger errorsDuringElection = new AtomicInteger(0);

        final VoteRequest voteRequest = new VoteRequest(
                clusterInfo.getCurrentNode().getTerm(),
                clusterInfo.getCurrentNode().getNodeId(),
                clusterInfo.getCurrentNode().getLastLogIndex(),
                clusterInfo.getCurrentNode().getLastLogTerm());

        this.voteTimeOutHandler = clusterInfo.getOtherNodes().stream()
                .map(otherNode -> executor.submit(
                        () -> otherNode.getGrpcClient().requestVote(voteRequest, VOTE_TIMEOUT_MILLIS))).toList();

        this.voteTimeOutHandler.forEach(future -> voteResultProcessing(future, VOTE_TIMEOUT_MILLIS + 1L, voteCounter, errorsDuringElection));

        if (voteCounter.get() >= clusterInfo.getMajoritySize()) {
            logger.info("Leader election --> voteThis = {}", voteCounter.get());
            return ElectionStatus.ELECTED;
        } else if(errorsDuringElection.get() == clusterInfo.getOtherNodeCount()) {
            return ElectionStatus.RESTART_ELECTION;
        } else {
            return ElectionStatus.ANOTHER_LEADER;
        }
    }

    private void voteResultProcessing(Future<VoteResponse> future, long timeOutMillis, AtomicInteger voteThis, AtomicInteger errorsDuringElection) {
        try {
            final VoteResponse voteResponse = future.get(timeOutMillis, TimeUnit.MILLISECONDS);
            if (voteResponse != null) {
                if (voteResponse.voteGranted()) {
                    voteThis.incrementAndGet();
                }
            }
        } catch (Exception e) {
            errorsDuringElection.incrementAndGet();
            logger.error("Error --> occurred due to timeout waiting for vote from other node.", e);
        }
    }

    @Override
    public void stopLeaderElection() {
        logger.info("Stopping leader election");
        this.voteTimeOutHandler.stream()
                .filter(Objects::nonNull)
                .filter(future -> !future.isDone())
                .forEach(future -> future.cancel(true));
    }
}
