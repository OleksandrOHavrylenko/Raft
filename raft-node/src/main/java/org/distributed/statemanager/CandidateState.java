package org.distributed.statemanager;

import org.distributed.model.ElectionStatus;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.election.ElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Oleksandr Havrylenko
 **/
public class CandidateState extends BaseState {
    private static final Logger logger = LoggerFactory.getLogger(CandidateState.class);

    private final State currentState = State.CANDIDATE;
    private int electionTimeoutMillis;
    private Timer electionTimer;
    private final ElectionService electionService;
    private final ClusterInfo clusterInfo;

    public CandidateState(final StateManager stateManager) {
        super(Objects.requireNonNull(stateManager));
        this.electionTimeoutMillis = getRandomIntInRange(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX);
        this.electionService = Objects.requireNonNull(stateManager.getElectionService());
        this.clusterInfo = Objects.requireNonNull(stateManager.getClusterInfo());
        this.onStart();
    }

    @Override
    public void onStart() {
        logger.info("Start --> CandidateState");
        clusterInfo.getCurrentNode().voteForSelfAndIncrTerm();

        final ElectionStatus electionStatus = this.electionService.startLeaderElection();
        switch (electionStatus) {
            case ELECTED -> nextState(new LeaderState(stateManager));
            case ANOTHER_LEADER -> nextState(new FollowerState(stateManager));
            case RESTART_ELECTION -> startElectionTimer();
        }

    }

    @Override
    public AppendEntriesResponse onHeartbeatFromLeader(final AppendEntriesRequest appendEntriesRequest) {
        logger.info("Heartbeat from Leader in CandidateState");
        if (appendEntriesRequest.term() == clusterInfo.getCurrentNode().getTerm()) {
            nextState(new FollowerState(stateManager));
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
            nextState(new FollowerState(stateManager));
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else {
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {

        if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(voteRequest.term(), voteRequest.candidateId());
            nextState(new FollowerState(stateManager));
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else if (voteRequest.term() == clusterInfo.getCurrentNode().getTerm()) {
            if (clusterInfo.getCurrentNode().getVotedFor() == null ||
                    clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId())) {
                nextState(new FollowerState(stateManager));
                return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
            }
        }

        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public void nextState(BaseState newState) {
        stopElectionTimer();
        this.stateManager.setState(newState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }

    private void startElectionTimer() {
        stopElectionTimer();

        logger.info("Starting ElectionTimer in CandidateState");
        final TimerTask startCandidateTask = new TimerTask() {
            @Override
            public void run() {
                nextState(new CandidateState(stateManager));
            }
        };
        this.electionTimer = new Timer();
        this.electionTimer.schedule(startCandidateTask, getRandomIntInRange(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX));
    }

    private void stopElectionTimer() {
        try {
            if (this.electionTimer != null) {
                this.electionTimer.cancel();
                this.electionTimer.purge();
            }
        } catch (Exception e) {
            logger.error("Error during stopping election timer", e);
        }
    }
}
