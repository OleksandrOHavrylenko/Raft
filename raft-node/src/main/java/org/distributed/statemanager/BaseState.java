package org.distributed.statemanager;

import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;

import java.util.Objects;
import java.util.Random;

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
    public abstract VoteResponse onRequestVote(final VoteRequest voteRequest);
    public void nextState(BaseState newState) {
        this.stateManager.setState(newState);
    }
    public abstract State getCurrentState();

    protected int getRandomIntInRange(int min, int max) {
        return new Random(System.currentTimeMillis()).nextInt(min, max + 1);
    }
}
