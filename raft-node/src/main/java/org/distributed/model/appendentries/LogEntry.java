package org.distributed.model.appendentries;

/**
 * @author Oleksandr Havrylenko
 **/
public record LogEntry(
        int index,
        long term,
        String command
) {
}
