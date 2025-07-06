package org.distributed.statemachine;

import org.distributed.service.election.ElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
@Component
public class StateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateManager.class);
    private BaseState currentState;
    private final ElectionService electionService;

    public StateManager(final ElectionService electionService) {
        this.currentState = new FollowerState(this);
        this.electionService = Objects.requireNonNull(electionService);
        LOGGER.info("StateManager created");
    }

    public void setState(final BaseState newState) {
        this.currentState = newState;
    }

    public ElectionService getElectionService() {
        return electionService;
    }
}
