package org.distributed.statemanager;

import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.message.MessageService;
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
    public AppendEntriesResponse onHeartbeatRequest(AppendEntriesRequest request) {
        logger.debug("Heartbeat from leader received in FollowerState");
        stopElectionTimeout();
        if (clusterInfo.getCurrentNode().getTerm() > request.term()) {
            startElectionTimeout(0L);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        if (request.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(request.term());
        }
        logger.info("request = {}", request);
        LogItem lastMessage = messageService.getByIndex(request.prevLogIndex());
        logger.info("lastMessage = {}, prevIndex = {}", lastMessage, IdGenerator.getPreviousIndex());
        if (lastMessage == null && IdGenerator.getPreviousIndex() >= 0) {
            startElectionTimeout(0L);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        if (isFirstItem(lastMessage) ||
                (lastMessage.id() == request.prevLogIndex() && lastMessage.term() == request.prevLogTerm())) {
            request.entries().stream()
                    .findFirst().ifPresent(message -> onMessageSave(message));
            IdGenerator.setLeaderCommit(request.leaderCommit());
            startElectionTimeout(0L);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        }
        startElectionTimeout(0L);
        return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    private static boolean isFirstItem(LogItem lastMessage) {
        return IdGenerator.getPreviousIndex() < 0 && lastMessage == null;
    }

    @Override
    public void onHeartbeatResponse(AppendEntriesResponse appendEntriesResponse, ClusterNode clusterNode) {
        logger.debug("Nothing to do in FollowerState");
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
        stopElectionTimeout();
        if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(voteRequest.term());
        }

        if (clusterInfo.getCurrentNode().getTerm() > voteRequest.term() ||
                (clusterInfo.getCurrentNode().getTerm() == voteRequest.term() &&
                    clusterInfo.getCurrentNode().getLastLogIndex() > voteRequest.lastLogIndex())) {
            startElectionTimeout(0L);
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
        } else if (voteRequest.term() == clusterInfo.getCurrentNode().getTerm()) {
            if ((clusterInfo.getCurrentNode().getVotedFor() == null ||
                    clusterInfo.getCurrentNode().getVotedFor().equals(voteRequest.candidateId()))
            && (clusterInfo.getCurrentNode().getLastLogIndex() <= voteRequest.lastLogIndex())) {
                clusterInfo.getCurrentNode().setVotedFor(voteRequest.candidateId());
                return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
            }
        }
        startElectionTimeout(0L);
        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public AppendEntriesResponse onReplicateRequest(final AppendEntriesRequest request) {
        stopElectionTimeout();
        if (clusterInfo.getCurrentNode().getTerm() > request.term()) {
            startElectionTimeout(0L);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        if (request.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(request.term());
        }

        LogItem lastMessage = messageService.getByIndex(request.prevLogIndex());
        if (lastMessage == null && IdGenerator.getPreviousIndex() >= 0) {
            logger.info("In null but Not first message, prevLogIndex = {}", request.prevLogIndex());
            startElectionTimeout(0L);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        if (isFirstItem(lastMessage) ||
                (lastMessage.id() == request.prevLogIndex() && lastMessage.term() == request.prevLogTerm())) {

            request.entries().stream()
                    .findFirst().ifPresent(message -> onMessageSave(message));
            IdGenerator.setLeaderCommit(request.leaderCommit());
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        }
        startElectionTimeout(0L);
        return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    private void onMessageSave(LogItem message) {
        IdGenerator.setId(message.id() + 1);
        messageService.saveMessages(message);
    }

    @Override
    public LogItem append(String message) {
        logger.info("Append denied in FollowerState, node = {}", clusterInfo.getCurrentNode().getNodeId());
        return null;
    }

    @Override
    public List<LogItem> getMessages() {
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
        stopElectionTimeout();
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
