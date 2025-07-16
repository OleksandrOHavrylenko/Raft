package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;

import java.util.Objects;
import java.util.Random;

/**
 * @author Oleksandr Havrylenko
 **/
public abstract class BaseState {
    public static final int ELECTION_TIMEOUT_MIN = 150;
    public static final int ELECTION_TIMOUT_MAX = 300;
    public static final int VOTE_TIMEOUT_MILLIS = 20;
    public static final int HEARTBEAT_INTERVAL = 70;

    protected final StateManager stateManager;

    public BaseState(final StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);
    }

    public abstract void onStart();
    public abstract AppendEntriesResponse onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest);
    public abstract VoteResponse onRequestVote(final VoteRequest voteRequest);
    public abstract void nextState(BaseState newState);
    public abstract State getCurrentState();

    protected int getRandomIntInRange(int min, int max) {
        return new Random(System.currentTimeMillis()).nextInt(min, max + 1);
    }
}
