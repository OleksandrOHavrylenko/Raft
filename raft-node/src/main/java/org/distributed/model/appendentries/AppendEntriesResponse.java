package org.distributed.model.appendentries;

/**
 * @author Oleksandr Havrylenko
 **/
public record AppendEntriesResponse(
        int term,
        boolean success
) {
}
