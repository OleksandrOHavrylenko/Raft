package org.distributed.statemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Oleksandr Havrylenko
 **/
public class FollowerState extends BaseState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowerState.class);

    private final State currentState = State.FOLLOWER;
    private int electionTimeoutMillis;
    private final Timer electionTimer = new Timer();

    public FollowerState(final StateManager stateManager) {
        super(stateManager);
        this.electionTimeoutMillis = getRandomIntInRange(1500, 3000);
        this.onStart();
    }

    @Override
    public void onStart() {
        LOGGER.info("Starting FollowerState");
        startElectionTimer();
    }



    @Override
    public void incomingHeartbeatFromLeader() {
        stopElectionTimer();
        startElectionTimer();

    }

    @Override
    public void enterState() {

    }

    @Override
    public void nextState(final BaseState newState) {
        stopElectionTimer();
        this.stateManager.setState(newState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }

    private void startElectionTimer() {
        final TimerTask startCandidateTask = new TimerTask() {
            @Override
            public void run() {
                nextState(new CandidateState(stateManager));
            }
        };
        electionTimer.schedule(startCandidateTask, electionTimeoutMillis);
    }

    private void stopElectionTimer() {
        electionTimer.cancel();
    }


}
