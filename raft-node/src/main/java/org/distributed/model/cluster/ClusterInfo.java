package org.distributed.model.cluster;

import org.distributed.model.ClusterNode;
import org.distributed.model.NodeInfo;
import org.distributed.repository.LogRepository;
import org.distributed.statemanager.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
@Component
public class ClusterInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterInfo.class);

    private final NodeInfo currentNode;
    private final List<ClusterNode> otherNodes;
    private State nodeState;

    public ClusterInfo(@Value("${node.id}") String nodeId, @Value("${grpc.server.port}") int port,
                       @Value("${grpc.node1.host}") String host1, @Value("${grpc.node1.port}") int port1,
                       @Value("${grpc.node2.host}") String host2, @Value("${grpc.node2.port}") int port2,
                       final LogRepository logRepository) {
        this.currentNode = new NodeInfo(nodeId, nodeId, port, logRepository);
        this.otherNodes = Arrays.asList(new ClusterNode(host1, host1, port1), new ClusterNode(host2, host2, port2));

        LOGGER.info("Cluster created on Node = {}, with other nodes = {}", this.currentNode, this.otherNodes);
    }

    public NodeInfo getCurrentNode() {
        return currentNode;
    }

    public int getClusterSize() {
        return otherNodes.size() + 1;
    }

    public int getMajoritySize() {
        return (getClusterSize() / 2) + 1;
    }

    public int getOtherNodeCount() {
        return otherNodes.size();
    }

    public List<ClusterNode> getOtherNodes() {
        return new ArrayList<>(otherNodes);
    }

    public synchronized State getNodeState() {
        return nodeState;
    }

    public synchronized void setNodeState(final State state) {
        this.nodeState = state;
    }
}
