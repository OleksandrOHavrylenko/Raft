package org.distributed.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
public class NodeInfo {
    private final String nodeId;
    private final String host;
    private final int port;
    private String votedFor = null;
    private final AtomicInteger term = new AtomicInteger(0);
    private final AtomicInteger lastLogIndex = new AtomicInteger(0);
    private final AtomicInteger lastLogTerm = new AtomicInteger(0);

    public NodeInfo(final String nodeId, final String host, final int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    public int getTerm() {
        return term.get();
    }

    public int getLastLogIndex() {
        return lastLogIndex.get();
    }

    public int getLastLogTerm() {
        return lastLogTerm.get();
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(final String votedFor) {
        this.votedFor = votedFor;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int incrementAndGet() {
        return term.incrementAndGet();
    }
    public void voteForSelfAndIncrTerm() {
        setVotedFor(nodeId);
        incrementAndGet();
    }

    public void setTerm(final int term) {
        setVotedFor(null);
        this.term.set(term);
    }

    public void setTerm(final int term, final String nodeId) {
        setVotedFor(nodeId);
        this.term.set(term);
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "nodeId='" + nodeId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", term=" + term +
                '}';
    }


}
