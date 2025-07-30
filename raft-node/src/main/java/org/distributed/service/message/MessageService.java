package org.distributed.service.message;

import org.distributed.model.dto.LogItem;

import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
public interface MessageService {
    LogItem append(final String message);
    List<String> getMessages();
    LogItem getLastMessage();
    void saveMessages(LogItem messages);
    long getTermByIndex(int index);
    LogItem getByIndex(int index);
}
