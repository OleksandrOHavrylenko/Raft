package org.distributed.statemachine;

import org.distributed.service.election.ElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
public class CandidateState extends BaseState{
    private static final Logger LOGGER = LoggerFactory.getLogger(CandidateState.class);
    private final State currentState = State.CANDIDATE;
    private final ElectionService electionService;

    public CandidateState(final StateManager stateManager) {
        super(Objects.requireNonNull(stateManager));
        this.electionService = Objects.requireNonNull(stateManager.getElectionService());
        this.onStart();
    }

    @Override
    public void onStart() {
        LOGGER.info("Starting CandidateState");
        this.electionService.startLeaderElection();
    }

    @Override
    public void incomingHeartbeatFromLeader() {

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
}
