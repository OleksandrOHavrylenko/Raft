package org.distributed.model;

import org.distributed.grpc.GrpcClient;
import org.distributed.grpc.GrpcClientImpl;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
public class ClusterNode {
    private final String nodeId;
    private final String host;
    private final int port;
    private GrpcClient grpcClient;


    public ClusterNode(final String nodeId, final String host, final int port) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.grpcClient = new GrpcClientImpl(this.host, this.port);
    }

    public GrpcClient getGrpcClient() {
        return grpcClient;
    }
}
