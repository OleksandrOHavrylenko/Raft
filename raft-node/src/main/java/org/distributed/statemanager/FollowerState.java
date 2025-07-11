package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
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
    public AppendEntriesResponse onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest) {
        logger.info("Heartbeat from leader received in FollowerState");
        stopElectionTimer();
        if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
            startElectionTimer();
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else if (appendEntriesRequest.term() == clusterInfo.getCurrentNode().getTerm()) {
            startElectionTimer();
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else {
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {

        if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(voteRequest.term(), voteRequest.candidateId());
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else if (voteRequest.term() == clusterInfo.getCurrentNode().getTerm()) {
            if (clusterInfo.getCurrentNode().getVotedFor() == null ||
                    clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId())) {
                return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
            }
        }

        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
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
        this.electionTimer.schedule(startCandidateTask, getRandomIntInRange(1500, 3000));
    }

    private void stopElectionTimer() {
        try {
            if (this.electionTimer != null) {
                this.electionTimer.purge();
                this.electionTimer.cancel();
            }
        } catch (Exception e) {
            logger.error("Error stopping election timer in Follower State", e);
        }
    }
}
