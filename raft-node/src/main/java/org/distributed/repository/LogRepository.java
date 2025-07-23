package org.distributed.repository;

import org.distributed.model.dto.LogItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
@Repository
public class LogRepository {
    private static final Logger log = LoggerFactory.getLogger(LogRepository.class);

    private final LogItem[] logsRepository = new LogItem[1000];

    public synchronized void add(final LogItem item) {
        logsRepository[item.id()] = item;
        log.info("Message saved to repository {}-{}", item.id(), item.message());
    }

    public List<String> getAll(int maxIndex) {
        log.info("Get all messages from 0 - {}", maxIndex);
        return Arrays.stream(logsRepository, 0, maxIndex).map(LogItem::message).toList();
    }
}
