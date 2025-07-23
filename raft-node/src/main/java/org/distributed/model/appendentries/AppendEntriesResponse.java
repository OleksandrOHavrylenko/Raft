package org.distributed.model.appendentries;

/**
 * @author Oleksandr Havrylenko
 **/
public record AppendEntriesResponse(
        long term,
        boolean success
) {
}
