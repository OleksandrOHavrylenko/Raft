package org.distributed.statemanager;

import org.distributed.exceptions.NotLeaderException;
import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.message.MessageService;
import org.distributed.stubs.ResponseVoteRPC;
import org.distributed.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * @author Oleksandr Havrylenko
 **/
public abstract class BaseState {
    protected static final Logger logger = LoggerFactory.getLogger(BaseState.class);

    public static final long STARTUP_DELAY = 5000L;
    public static final long ELECTION_TIMEOUT_MIN = 7000;
    public static final long ELECTION_TIMOUT_MAX = 10000;
    public static final long VOTE_TIMEOUT_MILLIS = 100L;
    public static final long HEARTBEAT_INTERVAL = 5000L;
    public static final long REPLICATE_TIMEOUT = 50L;

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
    public abstract VoteResponse onRequestVote(final VoteRequest voteRequest);
    public abstract void onResponseVote(final ResponseVoteRPC responseVoteRPC, ClusterNode clusterNode);
    public abstract List<LogItem> getMessages();
    public abstract void nextState(State nextState);
    public abstract AppendEntriesResponse onReplicateRequest(final AppendEntriesRequest request);
    public abstract void onAppendEntriesResponse(AppendEntriesResponse appendEntriesResponse, ClusterNode clusterNode);
    public abstract State getCurrentState();
    public abstract void onStop();

    public LogItem append(final String message) {
        logger.info("Append LogItem denied, node = {} in {} state", clusterInfo.getCurrentNode().getNodeId(), getCurrentState());
        throw new NotLeaderException(clusterInfo.getNodeState());
    }

    protected long getRandomLongInRange(long min, long max) {
        return new Random(System.nanoTime()).nextLong(min, max + 1L);
    }

    protected boolean isFirstItem(LogItem lastMessage, int prevLogIndexFromRequest) {
        return lastMessage == null && (prevLogIndexFromRequest == IdGenerator.getPreviousIndex());
    }
}
