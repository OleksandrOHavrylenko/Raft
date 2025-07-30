package org.distributed.model.appendentries;

import org.distributed.model.dto.LogItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
public record AppendEntriesRequest(
        long term,
        String leaderId,
        int prevLogIndex,
        long prevLogTerm,
        List<LogItem> entries,
        int leaderCommit){


    @Override
    public List<LogItem> entries() {
        return new ArrayList<>(entries);
    }
}
