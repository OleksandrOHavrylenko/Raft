package org.distributed.model;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
@Component
public class NodeInfo {
    private String nodeId = "node1";
//    private String nodeName;
//    private String ipAddress;
//    private int port;
    private AtomicInteger term = new AtomicInteger(0);

    public int incrementAndGet() {
        return term.incrementAndGet();
    }

    public String getNodeId() {
        return nodeId;
    }
}
