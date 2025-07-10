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
        electionTimeoutMillis = getRandomIntInRange(1500, 3000);
        this.electionService = Objects.requireNonNull(stateManager.getElectionService());
        this.clusterInfo = Objects.requireNonNull(stateManager.getClusterInfo());
        this.onStart();
    }

    @Override
    public void onStart() {
        logger.info("Starting CandidateState");
        final ElectionStatus electionStatus = this.electionService.startLeaderElection();
        switch (electionStatus) {
            case ELECTED -> nextState(new LeaderState(stateManager));
            case ANOTHER_LEADER -> nextState(new FollowerState(stateManager));
            case RESTART_ELECTION -> startElectionTimer();
        }
    }

    @Override
    public AppendEntriesResponse onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest) {
        logger.info("Heartbeat from Leader in CandidateState");
        if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
        }
        stopElectionTimer();
        nextState(new FollowerState(stateManager));
        return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
        int currentTerm = clusterInfo.getCurrentNode().getTerm();

        if (currentTerm > voteRequest.term()) {
            logger.info("False -> Requested Vote in Candidate state, Current term is greater than vote term");
            return new VoteResponse(currentTerm, false);
        } else if (currentTerm < voteRequest.term()) {
            clusterInfo.getCurrentNode().setTerm(voteRequest.term());
            nextState(new FollowerState(stateManager));
            logger.info("True -> Requested Vote in Candidate state, currentTerm < voteRequest.term()");
            return new VoteResponse(currentTerm, true);
        } else if (clusterInfo.getCurrentNode().getVotedFor() == null ||
                clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId())) {
            nextState(new FollowerState(stateManager));
            logger.info("True -> Requested Vote in Candidate state");
            return new VoteResponse(currentTerm, true);
        } else {
            logger.info("False, because else -> Requested Vote in Candidate state. votedFor = {} , candidateId = {}",
                    clusterInfo.getCurrentNode().getVotedFor(), voteRequest.candidateId());
            return new VoteResponse(currentTerm, false);
        }
    }

    @Override
    public void nextState(BaseState newState) {
        stopElectionTimer();
        stateManager.setState(newState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }

    private void startElectionTimer() {
        logger.info("Starting ElectionTimer in CandidateState");
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
                this.electionTimer.cancel();
                this.electionTimer.purge();
            }
        } catch (Exception e) {
            logger.error("Error during stopping election timer", e);
        }
    }
}
