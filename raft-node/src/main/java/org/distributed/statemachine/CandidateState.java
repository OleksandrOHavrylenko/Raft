package org.distributed.statemachine;

import org.distributed.model.ElectionStatus;
import org.distributed.service.election.ElectionService;
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

    public CandidateState(final StateManager stateManager) {
        super(Objects.requireNonNull(stateManager));
        electionTimeoutMillis = getRandomIntInRange(150, 300);
        this.electionService = Objects.requireNonNull(stateManager.getElectionService());
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
    public void enterState() {

    }

    @Override
    public void nextState(BaseState state) {

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
