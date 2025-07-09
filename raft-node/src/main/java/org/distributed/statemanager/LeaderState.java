package org.distributed.statemanager;

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
        logger.info("Leader new Leader elected as NodeId: {}!", clusterInfo.getCurrentNode().getNodeId());
        onStart();
    }

    @Override
    public void onStart() {
        logger.info("Starting LeaderState");
        heartBeatService.startHeartBeatSchedule();
    }

    @Override
    public void onHeartbeatFromLeader() {
        logger.info("!!!Leader received Heartbeat from Leader");
        nextState(new FollowerState(stateManager));
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
        int currentTerm = clusterInfo.getCurrentNode().getTerm();

        if (currentTerm > voteRequest.term()) {
            logger.info("False -> Requested Vote in Candidate state, Current term is greater than vote term");
            return new VoteResponse(currentTerm, false);
        } else if (clusterInfo.getCurrentNode().getVotedFor() == null ||
                clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId())) {
            logger.info("True -> Requested Vote in Candidate state");
            nextState(new FollowerState(stateManager));
            return new VoteResponse(currentTerm, true);
        } else {
            logger.info("False, because else-> Requested Vote in Candidate state");
            return new VoteResponse(currentTerm, false);
        }
    }

    @Override
    public void nextState(BaseState newState) {
        heartBeatService.shutDownHeartBeats();
        super.nextState(newState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }
}
