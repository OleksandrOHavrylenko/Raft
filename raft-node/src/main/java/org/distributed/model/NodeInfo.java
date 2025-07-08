package org.distributed.model;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
public class NodeInfo {
    private String nodeId = "node1";
    private String host;
    private int port;
    private AtomicInteger term = new AtomicInteger(0);

    public NodeInfo(final String nodeId, final String host, final int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    public int getTerm() {
        return term.get();
    }
    public int incrementAndGet() {
        return term.incrementAndGet();
    }

    public String getNodeId() {
        return nodeId;
    }
}
