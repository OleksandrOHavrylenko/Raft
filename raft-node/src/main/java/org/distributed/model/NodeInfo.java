package org.distributed.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Oleksandr Havrylenko
 **/
public class NodeInfo {
    private final String nodeId;
    private final String host;
    private final int port;
    private String votedFor = null;
    private final AtomicLong term = new AtomicLong(0L);
    private final AtomicLong lastLogIndex = new AtomicLong(0L);
    private final AtomicLong lastLogTerm = new AtomicLong(0L);

    public NodeInfo(final String nodeId, final String host, final int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    public long getTerm() {
        return term.get();
    }

    public long getLastLogIndex() {
        return lastLogIndex.get();
    }

    public long getLastLogTerm() {
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

    public long incrementAndGet() {
        return term.incrementAndGet();
    }
    public void voteForSelfAndIncrTerm() {
        setVotedFor(nodeId);
        incrementAndGet();
    }

    public void setTerm(final long term) {
        setVotedFor(null);
        this.term.set(term);
    }

    public void setTerm(final long term, final String nodeId) {
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
