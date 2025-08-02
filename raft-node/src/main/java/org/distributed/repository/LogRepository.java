package org.distributed.repository;

import org.distributed.model.dto.LogItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Oleksandr Havrylenko
 **/
@Repository
public class LogRepository {
    private static final Logger logger = LoggerFactory.getLogger(LogRepository.class);

    private final LogItem[] logsRepository = new LogItem[1000];
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public void add(final LogItem item) {
        try {
            readWriteLock.writeLock().lock();
            logsRepository[item.id()] = item;
            logger.debug("Message saved to repository {}-{}", item.id(), item.message());
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public LogItem getLogItem(final int index) {
        try {
            readWriteLock.readLock().lock();
            return logsRepository[index];
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public List<LogItem> getAll(int maxIndex) {
        try {
            readWriteLock.readLock().lock();
            return Arrays.stream(logsRepository, 0, maxIndex).toList();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public LogItem getMessageByIndex(int index) {
        try {
            readWriteLock.readLock().lock();
            logger.info("get index = {}", index);
            if (index >= 0) {
                return logsRepository[index];
            }
            return null;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void eraseByIndex(int getIndex) {
        try {
            readWriteLock.writeLock().lock();
            logsRepository[getIndex] = null;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
