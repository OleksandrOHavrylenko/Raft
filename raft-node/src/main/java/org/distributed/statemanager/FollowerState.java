package org.distributed.statemanager;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;
import java.util.Timer;
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
    private int electionTimeoutMillis;
    private Timer electionTimer;
    private final ClusterInfo clusterInfo;
    private volatile boolean isTimeForElection = false;
    private ScheduledFuture<?> timeOutHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();

    public FollowerState(final StateManager stateManager, final ClusterInfo clusterInfo) {
        super(stateManager);
        this.electionTimeoutMillis = getRandomIntInRange(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX);
        logger.debug("electionTimeoutMillis = " + this.electionTimeoutMillis);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
//        this.onStart();
    }

    @Override
    public void onStart() {
        logger.info("Start --> FollowerState");
//        startElectionTimer();
        startElectionTimeout();
    }

    @Override
    public void onHeartbeatFromLeader(AppendEntriesRequest appendEntriesRequest) {
        logger.info("Heartbeat from leader received in FollowerState");
//        stopElectionTimer();
        stopElectionTimeout();
        if (appendEntriesRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            clusterInfo.getCurrentNode().setTerm(appendEntriesRequest.term());
        }
        startElectionTimeout();
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {
//        stopElectionTimer();
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

//        startElectionTimer();
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
//        stopElectionTimer();
//        shutdown();
        stopElectionTimeout();
//        shutdown();
    }

//    private void startElectionTimer() {
////        stopElectionTimer();
//        logger.info("Starting election timer in FollowerState");
//        final TimerTask startCandidateTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (isTimeForElection) {
//                    logger.info("Time is Out Follower --> Candidate");
//                    nextState(State.CANDIDATE);
//                }
//            }
//        };
//        if (this.electionTimer != null) {
//            this.electionTimer.cancel();
//        }
//        this.electionTimer = new Timer();
//        this.isTimeForElection = true;
//        int randomIntInRange = getRandomIntInRange(ELECTION_TIMEOUT_MIN, ELECTION_TIMOUT_MAX);
//        logger.info("Election timer intRange = {}", randomIntInRange);
//        this.electionTimer.schedule(startCandidateTask, randomIntInRange);
//    }
//
//    private void stopElectionTimer() {
//        logger.info("Stopping election timer in FollowerState");
//        this.isTimeForElection = false;
//        try {
//            if (this.electionTimer != null) {
//                this.electionTimer.cancel();
//            }
//        } catch (Exception e) {
//            logger.error("Error stopping election timer in Follower State", e);
//        }
//    }

    public void startElectionTimeout() {
        int timeout = ELECTION_TIMEOUT_MIN + random.nextInt(ELECTION_TIMOUT_MAX - ELECTION_TIMEOUT_MIN);
        logger.info("Election timer intRange = {}", timeout);
        timeOutHandler = scheduler.schedule(this::onElectionTimeout, timeout, TimeUnit.MILLISECONDS);
    }
//
    public void stopElectionTimeout() {
        logger.info("Trying to reset election timeout ...");
        if (timeOutHandler != null && !timeOutHandler.isDone()) {
            timeOutHandler.cancel(false);
            logger.info("Election timeout reset.");
        }
    }
//
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
