package org.distributed.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
public class IdGenerator {
    private final static AtomicInteger counter = new AtomicInteger(0);
    private final static AtomicInteger prevCounter = new AtomicInteger(-1);
    private final static AtomicInteger leaderCommit = new AtomicInteger(-1);

    public static int id() {
        int id = counter.getAndIncrement();
        prevCounter.set(id - 1);
        return id;
    }

    public static int getNextIndex() {
        return counter.get();
    }

    public static int getPreviousIndex() {
        return prevCounter.get();
    }

    public static int getLeaderCommit() {
        return leaderCommit.get();
    }

    public static void setLeaderCommit(int commit) {
        leaderCommit.set(commit);
    }


    public static void setId(int id) {
        counter.set(id);
        prevCounter.set(id - 1);
    }
}