package org.distributed.service.election;

import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.distributed.statemanager.BaseState.VOTE_TIMEOUT_MILLIS;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("electionService")
public class ElectionServiceImpl implements ElectionService {
    private static final Logger logger = LoggerFactory.getLogger(ElectionServiceImpl.class);

    private final ClusterInfo clusterInfo;
    private List<? extends Future<?>> voteTimeOutHandler;
    private final ExecutorService executor;

    public ElectionServiceImpl(final ClusterInfo clusterInfo) {
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.executor = Executors.newFixedThreadPool(clusterInfo.getOtherNodeCount());
    }

    @Override
    public void startLeaderElection() {
        logger.info("Starting leader election");

        final VoteRequest voteRequest = new VoteRequest(
                clusterInfo.getCurrentNode().getTerm(),
                clusterInfo.getCurrentNode().getNodeId(),
                clusterInfo.getCurrentNode().getLastLogIndex(),
                clusterInfo.getCurrentNode().getLastLogTerm());

        this.voteTimeOutHandler = clusterInfo.getOtherNodes().stream()
                .map(otherNode -> executor.submit(
                        () -> otherNode.getGrpcClient().requestVote(voteRequest, VOTE_TIMEOUT_MILLIS))).toList();
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
