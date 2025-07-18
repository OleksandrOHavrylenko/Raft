package org.distributed.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
public class IdGenerator {
    private final static AtomicInteger counter = new AtomicInteger(0);

    public static int id() {
        return counter.getAndIncrement();
    }

    public static int getLast() {
        return counter.get();
    }
}