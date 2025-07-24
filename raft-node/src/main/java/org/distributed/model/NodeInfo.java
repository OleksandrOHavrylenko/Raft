package org.distributed.model;

import org.distributed.repository.LogRepository;
import org.distributed.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Oleksandr Havrylenko
 **/
public class NodeInfo {
    private static final Logger logger = LoggerFactory.getLogger(NodeInfo.class);
    private final String nodeId;
    private final String host;
    private final int port;
    private String votedFor = null;
    private final AtomicLong term = new AtomicLong(0L);
    private final AtomicInteger lastLogIndex = new AtomicInteger(0);
    private final AtomicLong lastLogTerm = new AtomicLong(0L);
    private final AtomicInteger nextLogIndex = new AtomicInteger(1);
    private final LogRepository logRepository;

    public NodeInfo(final String nodeId, final String host, final int port, final LogRepository logRepository) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.logRepository = Objects.requireNonNull(logRepository);
    }

    public long getTerm() {
        return term.get();
    }

    public int getLastLogIndex() {
        return IdGenerator.getLast() - 1;
    }

    public long getLastLogTerm() {
        int lastLogIndex = getLastLogIndex();
        if (lastLogIndex < 0) {
            return 0L;
        }
        return logRepository.getLogItem(lastLogIndex).term();
    }

    public int getPrevLogIndex() {
        return IdGenerator.getPreviousIndex();
    }

    public long getPrevLogTerm() {
        int prevLogIndex = getPrevLogIndex();
        if (prevLogIndex < 0) {
            return 0L;
        }
        return logRepository.getLogItem(prevLogIndex).term();
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

    public int getNextLogIndex() {
        return nextLogIndex.get();
    }

    public void setNextLogIndex(final int index) {
        nextLogIndex.set(index);
    }

    public void voteForSelfAndIncrTerm() {
        setVotedFor(nodeId);
        incrementAndGet();
    }

    public void setTerm(final long term) {
        setTerm(term, null);
    }

    public synchronized void setTerm(final long term, final String nodeId) {
        setVotedFor(nodeId);
        this.term.set(term);
    }

    public int getLeaderCommit() {
        return IdGenerator.getLeaderCommit();
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
