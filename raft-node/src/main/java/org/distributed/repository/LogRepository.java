package org.distributed.repository;

import org.distributed.model.dto.LogItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oleksandr Havrylenko
 **/
@Repository
public class LogRepository {
    private static final Logger loggger = LoggerFactory.getLogger(LogRepository.class);

    private final LogItem[] logsRepository = new LogItem[1000];
    private final AtomicInteger logIndex = new AtomicInteger(0);

    public synchronized void add(final LogItem item) {
        if (logIndex.get() == item.id()) {
            logsRepository[item.id()] = item;
            logIndex.incrementAndGet();
            loggger.info("Message saved to repository {}-{}", item.id(), item.message());
        } else {
            loggger.error("Message not saved to repository id={}-{}, id should be {}", item.id(), item.message(), logIndex.get());
        }
    }

    public synchronized LogItem getLogItem(final int index) {
        if (index >= 0 || index < logIndex.get() - 1) {
            return logsRepository[index];
        } else {
            loggger.error("Incorrect index passed to repository index={}, should be < {}", index, logIndex.get());
            return null;
        }
    }

    public synchronized LogItem getLassMessage() {
        return logsRepository[logIndex.get() - 1];
    }

    public List<String> getAll(int maxIndex) {
        loggger.error("Max index passed to repository index={}, should be {}", maxIndex, logIndex.get());
        loggger.info("Get all messages from 0 - {}", maxIndex);
        return Arrays.stream(logsRepository, 0, maxIndex).map(LogItem::message).toList();
    }
}
