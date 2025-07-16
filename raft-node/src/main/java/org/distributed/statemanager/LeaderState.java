package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
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

    public LeaderState(final StateManager stateManager, final HeartBeatService heartBeatService, final ClusterInfo clusterInfo) {
        super(stateManager);
        this.heartBeatService = Objects.requireNonNull(heartBeatService);;
        this.clusterInfo = Objects.requireNonNull(clusterInfo);

//        onStart();
    }

    @Override
    public void onStart() {
        logger.info("Starting Leader at NodeId: {}! with Term = {}", clusterInfo.getCurrentNode().getNodeId(), clusterInfo.getCurrentNode().getTerm());
        heartBeatService.startHeartBeatSchedule();
    }

    @Override
    public void onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest) {
        logger.info("!!!Leader received Heartbeat from Leader");

        if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
            this.heartBeatService.shutDownHeartBeats();
            this.nextState(State.FOLLOWER);
        }
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {

        if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            this.clusterInfo.getCurrentNode().setTerm(voteRequest.term(), voteRequest.candidateId());
            this.heartBeatService.shutDownHeartBeats();
            this.nextState(State.FOLLOWER);
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
        }

        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public void nextState(State nextState) {
        logger.info("Leader goes to nextState = {}", nextState);
        this.stateManager.setState(nextState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }

    @Override
    public void onStop() {
        this.heartBeatService.shutDownHeartBeats();
    }
}
