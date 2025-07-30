package org.distributed.statemanager;

import org.distributed.model.ClusterNode;
import org.distributed.model.ElectionStatus;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.election.ElectionService;
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
public class CandidateState extends BaseState {
    private static final Logger logger = LoggerFactory.getLogger(CandidateState.class);

    private final State currentState = State.CANDIDATE;
    private final ElectionService electionService;
    private final ClusterInfo clusterInfo;
    private ScheduledFuture<?> timeOutHandler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CandidateState(final StateManager stateManager, final ElectionService electionService,
                          final MessageService messageService, final ClusterInfo clusterInfo) {
        super(stateManager, messageService, clusterInfo);
        this.electionService = Objects.requireNonNull(electionService);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
    }

    @Override
    public void onStart() {
        logger.info("Start --> CandidateState");
        clusterInfo.getCurrentNode().voteForSelfAndIncrTerm();

        final ElectionStatus electionStatus = this.electionService.startLeaderElection();
        switch (electionStatus) {
            case ELECTED -> nextState(State.LEADER);
            case ANOTHER_LEADER -> nextState(State.FOLLOWER);
            case RESTART_ELECTION -> startElectionTimeout();
        }
    }

    @Override
    public AppendEntriesResponse onHeartbeatRequest(final AppendEntriesRequest request) {
        logger.debug("Heartbeat from Leader in CandidateState");
        LogItem lastMessage = messageService.getByIndex(request.prevLogIndex());
        if (lastMessage == null || (lastMessage.id() == request.prevLogIndex() && lastMessage.term() == request.prevLogTerm())) {
            stopElectionTimeout();
            IdGenerator.setId(Math.min(IdGenerator.getNextIndex(), request.prevLogIndex() + 1));
            request.entries().stream()
                    .findFirst().ifPresent((messageService::saveMessages));
            IdGenerator.setLeaderCommit(request.leaderCommit());
            nextState(State.FOLLOWER);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        } else if (request.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(request.term());
        }
        stopElectionTimeout();
        nextState(State.FOLLOWER);
        return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public void onHeartbeatResponse(AppendEntriesResponse appendEntriesResponse, ClusterNode clusterNode) {
        logger.debug("Nothing to do in CandidateState");
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
        stopElectionTimeout();

        if (clusterInfo.getCurrentNode().getTerm() > voteRequest.term() ||
                (clusterInfo.getCurrentNode().getTerm() == voteRequest.term() &&
                        clusterInfo.getCurrentNode().getLastLogIndex() > voteRequest.lastLogIndex())) {
            startElectionTimeout();
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
        } else if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(voteRequest.term());
            nextState(State.FOLLOWER);
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }

        startElectionTimeout();
        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public AppendEntriesResponse onReplicateRequest(final AppendEntriesRequest request) {
        stopElectionTimeout();
        LogItem lastMessage = messageService.getByIndex(IdGenerator.getLeaderCommit());
        if (lastMessage == null || (lastMessage.id() == request.prevLogIndex() && lastMessage.term() == request.prevLogTerm())) {
            request.entries().stream()
                    .findFirst().ifPresent((messageService::saveMessages));
            IdGenerator.setLeaderCommit(request.leaderCommit());
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), true);
        }
        startElectionTimeout();
        return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public LogItem append(String message) {
        logger.info("Append denied in CandidateState, node = {}", clusterInfo.getCurrentNode().getNodeId());
        return null;
    }

    @Override
    public List<String> getMessages() {
        return messageService.getMessages();
    }

    @Override
    public void nextState(State nextState) {
        stopElectionTimeout();
        electionService.stopLeaderElection();
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

    public void startElectionTimeout() {
        long timeout = new Random(System.nanoTime()).nextLong(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX);
        logger.info("Starting ElectionTimer in CandidateState with timeout: {}",timeout);
        timeOutHandler = scheduler.schedule(this::onElectionTimeout, timeout, TimeUnit.MILLISECONDS);
    }
    public void stopElectionTimeout() {
        if (timeOutHandler != null && !timeOutHandler.isDone()) {
            timeOutHandler.cancel(false);
            logger.info("Election timeout reset in CandidateState.");
        }
    }
    private void onElectionTimeout() {
        logger.info("Time is Out Candidate --> Candidate");
        nextState(State.CANDIDATE);
    }

    public void shutdown() {
        logger.info("Stopping election timer in CandidateState");
        scheduler.shutdownNow();
    }
}
