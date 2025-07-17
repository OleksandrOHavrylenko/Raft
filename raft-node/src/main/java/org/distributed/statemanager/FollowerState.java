package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private long electionTimeoutMillis;
    private final ClusterInfo clusterInfo;
    private ScheduledFuture<?> timeOutHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random(System.nanoTime());

    public FollowerState(final StateManager stateManager, final ClusterInfo clusterInfo) {
        super(stateManager);
        this.electionTimeoutMillis = getRandomLongInRange(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX);
        logger.debug("electionTimeoutMillis = " + this.electionTimeoutMillis);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
    }

    @Override
    public void onStart() {
        logger.info("Start --> FollowerState");
        startElectionTimeout();
    }

    @Override
    public void onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest) {
        logger.info("Heartbeat from leader received in FollowerState");
        stopElectionTimeout();
        if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
        }
        startElectionTimeout();
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
                return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), true);
            }
        }

        startElectionTimeout();
        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
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

    public void startElectionTimeout() {
        long timeout = random.nextLong(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX);
        logger.info("Election timer intRange = {}", timeout);
        timeOutHandler = scheduler.schedule(this::onElectionTimeout, timeout, TimeUnit.MILLISECONDS);
    }
    public void stopElectionTimeout() {
        logger.info("Trying to reset election timeout ...");
        if (timeOutHandler != null && !timeOutHandler.isDone()) {
            timeOutHandler.cancel(false);
            logger.info("Election timeout reset.");
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
