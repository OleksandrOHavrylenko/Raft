package org.distributed.statemanager;

import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Oleksandr Havrylenko
 **/
public class FollowerState extends BaseState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowerState.class);

    private final State currentState = State.FOLLOWER;
    private int electionTimeoutMillis;
    private final Timer electionTimer = new Timer();
    private final ClusterInfo clusterInfo;

    public FollowerState(final StateManager stateManager) {
        super(stateManager);
        this.electionTimeoutMillis = getRandomIntInRange(15000, 30000);
        LOGGER.debug("electionTimeoutMillis = " + this.electionTimeoutMillis);
        this.clusterInfo = Objects.requireNonNull(stateManager.getClusterInfo());
        this.onStart();
    }

    @Override
    public void onStart() {
        LOGGER.info("Starting FollowerState");
        startElectionTimer();
    }

    @Override
    public void incomingHeartbeatFromLeader() {
        stopElectionTimer();
        startElectionTimer();
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
        int currentTerm = clusterInfo.getCurrentNode().getTerm();

        if (currentTerm > voteRequest.term()) {
            return new VoteResponse(currentTerm, false);
        } else if (clusterInfo.getCurrentNode().getVotedFor() == null ||
                clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId())) {
            return new VoteResponse(currentTerm, true);
        } else {
            return new VoteResponse(currentTerm, false);
        }
    }

    @Override
    public void nextState(final BaseState newState) {
        stopElectionTimer();
        this.stateManager.setState(newState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }

    private void startElectionTimer() {
        final TimerTask startCandidateTask = new TimerTask() {
            @Override
            public void run() {
                nextState(new CandidateState(stateManager));
            }
        };
        electionTimer.schedule(startCandidateTask, electionTimeoutMillis);
    }

    private void stopElectionTimer() {
        electionTimer.cancel();
    }


}
