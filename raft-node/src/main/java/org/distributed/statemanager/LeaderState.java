package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.heartbeat.HeartBeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
public class LeaderState extends BaseState{
    private static final Logger logger = LoggerFactory.getLogger(LeaderState.class);
    private final State currentState = State.LEADER;
    private final HeartBeatService heartBeatService;
    private final ClusterInfo clusterInfo;

    public LeaderState(final StateManager stateManager) {
        super(stateManager);
        this.heartBeatService = Objects.requireNonNull(stateManager.getHeartBeatService());
        this.clusterInfo = Objects.requireNonNull(stateManager.getClusterInfo());
        logger.info("Leader new Leader elected as NodeId: {}! with Term = {}", clusterInfo.getCurrentNode().getNodeId(), clusterInfo.getCurrentNode().getTerm());
        onStart();
    }

    @Override
    public void onStart() {
        logger.info("Starting LeaderState");
        heartBeatService.startHeartBeatSchedule();
    }

    @Override
    public AppendEntriesResponse onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest) {
        logger.info("!!!Leader received Heartbeat from Leader");

        if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
            this.nextState(new FollowerState(stateManager));
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else {
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {

        if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            this.clusterInfo.getCurrentNode().setTerm(voteRequest.term(), voteRequest.candidateId());
            this.nextState(new FollowerState(stateManager));
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
        }

        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public void nextState(BaseState newState) {
        logger.info("Leader goes to nextState = {}", newState);
        this.heartBeatService.shutDownHeartBeats();
        this.stateManager.setState(newState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }
}
