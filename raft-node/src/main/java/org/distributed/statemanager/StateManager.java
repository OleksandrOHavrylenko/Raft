package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
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
    private final ElectionService electionService;
    private final HeartBeatService heartBeatService;
    private final ClusterInfo clusterInfo;

    public StateManager(final ElectionService electionService, final ClusterInfo clusterInfo,
                        final HeartBeatService heartBeatService) {
        this.electionService = Objects.requireNonNull(electionService);
        this.heartBeatService = Objects.requireNonNull(heartBeatService);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);

//        Should be after clusterInfo initialization
        this.currentState = new FollowerState(this);
        LOGGER.info("StateManager created");
    }

    public void setState(final BaseState newState) {
        this.currentState = newState;
    }

    public ElectionService getElectionService() {
        return electionService;
    }

    public HeartBeatService getHeartBeatService() {
        return heartBeatService;
    }

    public ClusterInfo getClusterInfo() {
        return clusterInfo;
    }

    public VoteResponse requestVote(final VoteRequest voteRequest) {
        return currentState.onRequestVote(voteRequest);
    }

    public AppendEntriesResponse onHeartbeatFromLeader (AppendEntriesRequest appendEntriesRequest) {
        return currentState.onHeartbeatFromLeader(appendEntriesRequest);
    }

    public State getCurrentState() {
        return currentState.getCurrentState();
    }
}
