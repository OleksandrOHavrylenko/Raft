package org.distributed.model;

import org.distributed.grpc.GrpcClient;
import org.distributed.grpc.GrpcClientImpl;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.dto.LogItem;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author Oleksandr Havrylenko
 **/
public class ClusterNode {
    private final String nodeId;
    private final String host;
    private final int port;
    private final GrpcClient grpcClient;


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

    @Override
    public String toString() {
        return "ClusterNode{" +
                "nodeId='" + nodeId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
