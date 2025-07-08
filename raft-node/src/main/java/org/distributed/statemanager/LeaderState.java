package org.distributed.statemanager;

import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
public class LeaderState extends BaseState{
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderState.class);
    private final State currentState = State.LEADER;
    private final ClusterInfo clusterInfo;

    public LeaderState(final StateManager stateManager) {
        super(stateManager);
        this.clusterInfo = Objects.requireNonNull(stateManager.getClusterInfo());
        LOGGER.info("Leader new Leader elected as NodeId: {}!", clusterInfo.getCurrentNode().getNodeId());
        onStart();
    }

    @Override
    public void onStart() {
        LOGGER.info("Starting LeaderState");

    }

    @Override
    public void incomingHeartbeatFromLeader() {

    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
        int currentTerm = clusterInfo.getCurrentNode().getTerm();

        if (currentTerm > voteRequest.term()) {
            return new VoteResponse(currentTerm, false);
        } else if (clusterInfo.getCurrentNode().getVotedFor() == null ||
                clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId())) {
            nextState(new FollowerState(stateManager));
            return new VoteResponse(currentTerm, true);
        } else {
            return new VoteResponse(currentTerm, false);
        }
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }
}
