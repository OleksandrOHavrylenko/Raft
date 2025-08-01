package org.distributed.statemanager;

import org.distributed.exceptions.NotLeaderException;
import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.election.ElectionService;
import org.distributed.service.message.MessageService;
import org.distributed.stubs.ResponseVoteRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    final AtomicInteger voteThis = new AtomicInteger(1);
    final AtomicInteger voteOther = new AtomicInteger(0);
    final AtomicInteger errorsDuringElection = new AtomicInteger(0);

    public CandidateState(final StateManager stateManager, final ElectionService electionService,
                          final MessageService messageService, final ClusterInfo clusterInfo) {
        super(stateManager, messageService, clusterInfo);
        this.electionService = Objects.requireNonNull(electionService);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
    }

    @Override
    public void onStart() {
        logger.info("Start --> CandidateState");
        resetCounters();

        clusterInfo.getCurrentNode().voteForSelfAndIncrTerm();

        this.electionService.startLeaderElection();
    }

    @Override
    public void onAppendEntriesResponse(AppendEntriesResponse appendEntriesResponse, ClusterNode clusterNode) {
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
    public void onResponseVote(final ResponseVoteRPC responseVoteRPC, final ClusterNode clusterNode) {
        if (responseVoteRPC != null) {
            if (responseVoteRPC.getTerm() > clusterInfo.getCurrentNode().getTerm()) {
                stopElectionTimeout();
                clusterInfo.getCurrentNode().setTerm(responseVoteRPC.getTerm());
                this.nextState(State.FOLLOWER);
            }

            if (responseVoteRPC.getVoteGranted()) {
                voteThis.incrementAndGet();
            } else {
                voteOther.incrementAndGet();
            }
        } else {
            errorsDuringElection.incrementAndGet();
        }

        decideNextStep();
    }

    private void decideNextStep() {
        if (voteThis.get() >= clusterInfo.getMajoritySize()) {
            nextState(State.LEADER);
        }
        if (errorsDuringElection.get() == clusterInfo.getMajoritySize()) {
            nextState(State.CANDIDATE);
        }
        if (voteThis.get() + voteOther.get() + errorsDuringElection.get() >= clusterInfo.getClusterSize()) {
            nextState(State.FOLLOWER);
        }
    }

    @Override
    public AppendEntriesResponse onReplicateRequest(final AppendEntriesRequest request) {
        stopElectionTimeout();
        if (clusterInfo.getCurrentNode().getTerm() > request.term()) {
            startElectionTimeout();
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        if (request.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(request.term());
            stopElectionTimeout();
            nextState(State.FOLLOWER);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        nextState(State.FOLLOWER);
        return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public LogItem append(String message) {
        logger.info("Append denied in CandidateState, node = {}", clusterInfo.getCurrentNode().getNodeId());
        throw new NotLeaderException(clusterInfo.getNodeState());
    }

    @Override
    public List<LogItem> getMessages() {
        return messageService.getMessages();
    }

    @Override
    public void nextState(State nextState) {
        stopElectionTimeout();
        resetCounters();
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

    private void resetCounters() {
        this.voteThis.set(1);
        this.voteOther.set(0);
        this.errorsDuringElection.set(0);
    }
}
