package org.distributed.model;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
public class ClusterNode {
    private final String nodeId;

    public ClusterNode(final String nodeId) {
        this.nodeId = Objects.requireNonNull(nodeId);
    }

    public String getNodeId() {
        return nodeId;
    }
}
