package org.distributed.model.appendentries;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
public record AppendEntriesRequest(
        int term,
        String leaderId,
        int prevLogIndex,
        int prevLogTerm,
        List<LogEntry> entries,
        int leaderCommit) {


    @Override
    public List<LogEntry> entries() {
        return new ArrayList<>(entries);
    }
}
