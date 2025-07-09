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
    private static final Logger logger = LoggerFactory.getLogger(FollowerState.class);

    private final State currentState = State.FOLLOWER;
    private int electionTimeoutMillis;
    private Timer electionTimer;
    private final ClusterInfo clusterInfo;

    public FollowerState(final StateManager stateManager) {
        super(stateManager);
        this.electionTimeoutMillis = getRandomIntInRange(1500, 3000);
        logger.debug("electionTimeoutMillis = " + this.electionTimeoutMillis);
        this.clusterInfo = Objects.requireNonNull(stateManager.getClusterInfo());
        this.onStart();
    }

    @Override
    public void onStart() {
        logger.info("Starting FollowerState");
        startElectionTimer();
    }

    @Override
    public void onHeartbeatFromLeader() {
        logger.info("Heartbeat from leader received in FollowerState");
        stopElectionTimer();

        startElectionTimer();
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
            return new VoteResponse(currentTerm, true);
        } else {
            logger.info("False, because else-> Requested Vote in Candidate state");
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
        logger.info("Starting election timer in FollowerState");
        final TimerTask startCandidateTask = new TimerTask() {
            @Override
            public void run() {
                nextState(new CandidateState(stateManager));
            }
        };
        this.electionTimer = new Timer();
        this.electionTimer.schedule(startCandidateTask, electionTimeoutMillis);
    }

    private void stopElectionTimer() {
        try {
            this.electionTimer.cancel();
            this.electionTimer.purge();
        } catch (Exception e) {
            logger.error("Error stopping election timer in Follower State", e);
        }
    }
}
