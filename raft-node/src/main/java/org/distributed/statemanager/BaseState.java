package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.message.MessageService;
import org.distributed.stubs.RequestAppendEntriesRPC;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * @author Oleksandr Havrylenko
 **/
public abstract class BaseState {
    public static final long STARTUP_DELAY = 7000L;
    public static final long ELECTION_TIMEOUT_MIN = 250;
    public static final long ELECTION_TIMOUT_MAX = 500;
    public static final long VOTE_TIMEOUT_MILLIS = 10L;
    public static final long HEARTBEAT_INTERVAL = 50L;

    protected final StateManager stateManager;
    protected final MessageService messageService;
    protected final ClusterInfo clusterInfo;

    public BaseState(final StateManager stateManager, final MessageService messageService,
                     final ClusterInfo clusterInfo) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.messageService = Objects.requireNonNull(messageService);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
    }

    public abstract void onStart();
    public abstract void onHeartbeatRequest(AppendEntriesRequest appendEntriesRequest);
    public abstract void onHeartbeatResponse(AppendEntriesResponse appendEntriesResponse);
    public abstract VoteResponse onRequestVote(final VoteRequest voteRequest);
    public abstract LogItem append(final String message);
    public abstract List<String> getMessages();
    public abstract void nextState(State nextState);
    public abstract State getCurrentState();
    public abstract void onStop();

    protected long getRandomLongInRange(long min, long max) {
        return new Random(System.nanoTime()).nextLong(min, max + 1L);
    }

    public abstract void onReplicateRequest(final AppendEntriesRequest request);
}
