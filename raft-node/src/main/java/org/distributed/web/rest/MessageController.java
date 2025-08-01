package org.distributed.web.rest;

import org.distributed.model.dto.LogItem;
import org.distributed.statemanager.State;
import org.distributed.statemanager.StateManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
@RestController
public class MessageController {

    private final StateManager stateManager;

    public MessageController(final StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);
    }

    @GetMapping("/list")
    public List<String> getMessages() {
        List<String> messages = stateManager.getMessages().stream().map(LogItem::message).toList();
        return messages;
    }

    @GetMapping("/state")
    public String getState() {
        return String.format("%s, term = %d.", stateManager.getCurrentState(), stateManager.getClusterInfo().getCurrentNode().getTerm());
    }

    @PostMapping("/append")
    public String append(@Nonnull @RequestBody String message) {
        final LogItem item = stateManager.append(message);
        return String.format("ACK %s", item.message());
    }
}
