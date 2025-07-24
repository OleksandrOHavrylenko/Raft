package org.distributed.model;

import org.distributed.grpc.GrpcClient;
import org.distributed.grpc.GrpcClientImpl;
import org.distributed.model.appendentries.AppendEntriesRequest;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
public class ClusterNode {
    private final String nodeId;
    private final String host;
    private final int port;
    private final GrpcClient grpcClient;
    private final AtomicInteger nextIndex = new AtomicInteger(0);


    public ClusterNode(final String nodeId, final String host, final int port) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.grpcClient = new GrpcClientImpl(this.host, this.port);
    }

    public GrpcClient getGrpcClient() {
        return grpcClient;
    }

    public void asyncSendMessage(final AppendEntriesRequest appendEntriesRequest, final CountDownLatch replicationDone, boolean waitForReady) {
        this.grpcClient.asyncReplicateLog(appendEntriesRequest, replicationDone, waitForReady);
    }

    public void setNextIndex(final int nextIndex) {
        this.nextIndex.set(nextIndex);
    }

    public int getNextIndex() {
        return nextIndex.get();
    }

    @Override
    public String toString() {
        return "ClusterNode{" +
                "nodeId='" + nodeId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
