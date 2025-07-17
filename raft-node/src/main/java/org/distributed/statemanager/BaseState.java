package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;

import java.util.Objects;
import java.util.Random;

/**
 * @author Oleksandr Havrylenko
 **/
public abstract class BaseState {
    public static final long ELECTION_TIMEOUT_MIN = 150;
    public static final long ELECTION_TIMOUT_MAX = 300;
    public static final long VOTE_TIMEOUT_MILLIS = 20;
    public static final long HEARTBEAT_INTERVAL = 50;

    protected final StateManager stateManager;

    public BaseState(final StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);
    }

    public abstract void onStart();
    public abstract void onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest);
    public abstract VoteResponse onRequestVote(final VoteRequest voteRequest);
    public abstract void nextState(State nextState);
    public abstract State getCurrentState();
    public abstract void onStop();

    protected long getRandomLongInRange(long min, long max) {
        return new Random(System.nanoTime()).nextLong(min, max + 1L);
    }
}
