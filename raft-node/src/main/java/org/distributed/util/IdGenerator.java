package org.distributed.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
public class IdGenerator {
    private final static AtomicInteger counter = new AtomicInteger(0);
    private final static AtomicInteger leaderCommit = new AtomicInteger(0);

    public static int id() {
        return counter.getAndIncrement();
    }

    public static int getLast() {
        return counter.get();
    }

    public static int getLeaderCommit() {
        return leaderCommit.get();
    }

    public static void setLeaderCommit(int commit) {
        leaderCommit.set(commit);
    }


}