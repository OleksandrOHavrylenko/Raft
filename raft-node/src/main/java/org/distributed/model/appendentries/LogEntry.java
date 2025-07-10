package org.distributed.model.appendentries;

/**
 * @author Oleksandr Havrylenko
 **/
public record LogEntry(
        int term,
        String command
) {
}
