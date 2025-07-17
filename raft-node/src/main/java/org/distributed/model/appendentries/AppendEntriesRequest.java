package org.distributed.model.appendentries;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
public record AppendEntriesRequest(
        long term,
        String leaderId,
        long prevLogIndex,
        long prevLogTerm,
        List<LogEntry> entries,
        long leaderCommit) {


    @Override
    public List<LogEntry> entries() {
        return new ArrayList<>(entries);
    }
}
