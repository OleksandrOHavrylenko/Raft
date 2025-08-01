package org.distributed.statemanager;

import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.election.ElectionService;
import org.distributed.service.heartbeat.HeartBeatService;
import org.distributed.service.message.MessageService;
import org.distributed.stubs.ResponseVoteRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
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
                        final MessageService messageService, final ClusterInfo clusterInfo) {
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.followerState = new FollowerState(this, messageService, this.clusterInfo);
        this.candidateState = new CandidateState(this, electionService, messageService, this.clusterInfo);
        this.leaderState = new LeaderState(this, heartBeatService, messageService, this.clusterInfo);

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

    public void onResponseVote(final ResponseVoteRPC responseVoteRPC, ClusterNode clusterNode) {
        currentState.onResponseVote(responseVoteRPC, clusterNode);
    }

    public void onAppendEntriesResponse(final AppendEntriesResponse appendEntriesResponse, ClusterNode clusterNode) {
        currentState.onAppendEntriesResponse(appendEntriesResponse, clusterNode);
    }

    public State getCurrentState() {
        return currentState.getCurrentState();
    }

    public ClusterInfo getClusterInfo () {
        return this.clusterInfo;
    }

    public List<LogItem> getMessages() {
        return this.currentState.getMessages();
    }

    public LogItem append(final String message) {
        return this.currentState.append(message);
    }

    public AppendEntriesResponse onReplicateRequest(final AppendEntriesRequest request) {
        return this.currentState.onReplicateRequest(request);
    }
}
