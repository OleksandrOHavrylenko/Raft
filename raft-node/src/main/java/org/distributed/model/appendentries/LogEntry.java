package org.distributed.model.appendentries;

/**
 * @author Oleksandr Havrylenko
 **/
public record LogEntry(
        long term,
        String command
) {
}
