package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.message.MessageService;
import org.distributed.stubs.RequestAppendEntriesRPC;
import org.distributed.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Oleksandr Havrylenko
 **/
public class FollowerState extends BaseState {
    private static final Logger logger = LoggerFactory.getLogger(FollowerState.class);

    private final State currentState = State.FOLLOWER;
    private final ClusterInfo clusterInfo;
    private ScheduledFuture<?> timeOutHandler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public FollowerState(final StateManager stateManager, final MessageService messageService,
                         final ClusterInfo clusterInfo) {
        super(stateManager, messageService, clusterInfo);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
    }

    @Override
    public void onStart() {
        logger.info("Start --> FollowerState");
        long delay = 0L;
        if (clusterInfo.getCurrentNode().getTerm() == 0L) {
            delay = STARTUP_DELAY;
        }
        startElectionTimeout(delay);
    }

    @Override
    public void onHeartbeatRequest(AppendEntriesRequest appendEntriesRequest) {
        logger.debug("Heartbeat from leader received in FollowerState");
        stopElectionTimeout();
        if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
        }
        startElectionTimeout(0L);
    }

    @Override
    public void onHeartbeatResponse(AppendEntriesResponse appendEntriesResponse) {
        logger.debug("Nothing to do in FollowerState");
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
        stopElectionTimeout();

        if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(voteRequest.term(), voteRequest.candidateId());
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else if (voteRequest.term() == clusterInfo.getCurrentNode().getTerm()) {
            if (clusterInfo.getCurrentNode().getVotedFor() == null ||
                    clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId())) {
                clusterInfo.getCurrentNode().setVotedFor(voteRequest.candidateId());
                return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
            }
        }

        startElectionTimeout(0L);
        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public void onReplicateRequest(RequestAppendEntriesRPC request) {
        stopElectionTimeout();
        final int id = IdGenerator.id();
        request.getEntriesList().stream()
                .map(entry -> new LogItem(entry.getIndex(), entry.getCommand(), entry.getTerm()))
                .findFirst().ifPresent((messageService::saveMessages));
        IdGenerator.setCommitCounter(request.getLeaderCommit());
        startElectionTimeout(0L);
    }

    @Override
    public LogItem append(String message) {
        logger.info("Append denied in FollowerState, node = {}", clusterInfo.getCurrentNode().getNodeId());
        return null;
    }

    @Override
    public List<String> getMessages() {
        return messageService.getMessages();
    }

    @Override
    public void nextState(final State nextState) {
            this.stateManager.setState(nextState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }

    @Override
    public void onStop() {
        stopElectionTimeout();
    }

    public void startElectionTimeout(long delay) {
        long timeout = new Random(System.nanoTime()).nextLong(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX) + delay;
        logger.debug("Election timer intRange = {}", timeout);
        timeOutHandler = scheduler.schedule(this::onElectionTimeout, timeout, TimeUnit.MILLISECONDS);
    }
    public void stopElectionTimeout() {
        logger.debug("Trying to reset election timeout ...");
        if (timeOutHandler != null && !timeOutHandler.isDone()) {
            timeOutHandler.cancel(false);
            logger.debug("Election timeout reset.");
        }
    }
    private void onElectionTimeout() {
        logger.info("Election timeout triggered. Becoming candidate...");
        logger.info("Time is Out Follower --> Candidate");
        nextState(State.CANDIDATE);
    }

    public void shutdown() {
        logger.info("Stopping election timer in FollowerState");
        scheduler.shutdownNow();
    }
}
