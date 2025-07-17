package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.election.ElectionService;
import org.distributed.service.heartbeat.HeartBeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
@Component
public class StateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateManager.class);
    private BaseState currentState;
    private final ClusterInfo clusterInfo;
    private final BaseState followerState;
    private final BaseState candidateState;
    private final BaseState leaderState;

    public StateManager(final ElectionService electionService, final HeartBeatService heartBeatService,
                        final ClusterInfo clusterInfo) {
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.followerState = new FollowerState(this, this.clusterInfo);
        this.candidateState = new CandidateState(this, electionService, this.clusterInfo);
        this.leaderState = new LeaderState(this, heartBeatService, this.clusterInfo);

        setState(State.FOLLOWER);
        LOGGER.debug("StateManager created");
    }

    public void setState(final State newState) {
        if (this.currentState != null) {
            this.currentState.onStop();
        }

        synchronized (this){
            this.clusterInfo.setNodeState(newState);
            switch (newState) {
                case FOLLOWER -> this.currentState = followerState;
                case CANDIDATE -> this.currentState = candidateState;
                case LEADER -> this.currentState = leaderState;
            }
        }

        if (this.currentState != null) {
            this.currentState.onStart();
        }
    }

    public VoteResponse requestVote(final VoteRequest voteRequest) {
        return currentState.onRequestVote(voteRequest);
    }

    public void onHeartbeatFromLeader (AppendEntriesRequest appendEntriesRequest) {
        currentState.onHeartbeatFromLeader(appendEntriesRequest);
    }

    public State getCurrentState() {
        return currentState.getCurrentState();
    }

    public ClusterInfo getClusterInfo () {
        return this.clusterInfo;
    }
}
