package org.distributed.service.message;

import org.distributed.model.dto.LogItem;

import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
public interface MessageService {
    LogItem append(final String message);
    List<String> getMessages();
}
