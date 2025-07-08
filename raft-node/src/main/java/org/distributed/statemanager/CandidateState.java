package org.distributed.statemanager;

import org.distributed.model.ElectionStatus;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.election.ElectionService;
import org.distributed.web.grpc.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Oleksandr Havrylenko
 **/
public class CandidateState extends BaseState{
    private static final Logger LOGGER = LoggerFactory.getLogger(CandidateState.class);
    private final State currentState = State.CANDIDATE;
    private int electionTimeoutMillis;
    private final Timer electionTimer = new Timer();
    private final ElectionService electionService;
    private final ClusterInfo clusterInfo;

    public CandidateState(final StateManager stateManager) {
        super(Objects.requireNonNull(stateManager));
        electionTimeoutMillis = getRandomIntInRange(150, 300);
        this.electionService = Objects.requireNonNull(stateManager.getElectionService());
        this.clusterInfo = Objects.requireNonNull(stateManager.getClusterInfo());
        this.onStart();
    }

    @Override
    public void onStart() {
        LOGGER.info("Starting CandidateState");
        ElectionStatus electionStatus = this.electionService.startLeaderElection();
        switch (electionStatus) {
            case ELECTED -> nextState(new LeaderState(stateManager));
            case ANOTHER_LEADER -> nextState(new FollowerState(stateManager));
            case RESTART_ELECTION -> startElectionTimer();
        }
    }

    @Override
    public void incomingHeartbeatFromLeader() {
        stopElectionTimer();

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
