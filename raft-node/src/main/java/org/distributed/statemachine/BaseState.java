package org.distributed.statemachine;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
public abstract class BaseState {
    protected final StateManager stateManager;

    public BaseState(final StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);
    }

    public abstract void onStart();
    public abstract void incomingHeartbeatFromLeader();
    public abstract void enterState();
    public abstract void nextState(BaseState state);
    public abstract State getCurrentState();
}
