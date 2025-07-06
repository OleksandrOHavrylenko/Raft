package org.distributed.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Oleksandr Havrylenko
 **/
public class LeaderState extends BaseState{
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderState.class);
    private final State currentState = State.LEADER;

    public LeaderState(StateManager stateManager) {
        super(stateManager);
    }

    @Override
    public void onStart() {
        LOGGER.info("Starting LeaderState");

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
