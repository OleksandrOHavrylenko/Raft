package org.distributed.grpc;

/**
 * @author Oleksandr Havrylenko
 **/
public interface Client {
    void requestVote();
    void appendEntries();
}
